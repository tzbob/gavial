package mtfrp.core

import akka.http.scaladsl.server._
import akka.stream._
import akka.stream.scaladsl._
import de.heikoseeberger.akkasse._
import hokko.{core => HC}
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object RouteCreator {
  import Directives._
  import EventStreamMarshalling._

  type SourceQueue[A] = SourceQueueWithComplete[A]
  type SseQueue       = SourceQueue[ServerSentEvent]
  type SseSource[A]   = Source[ServerSentEvent, A]

  def buildExitRoute[A](source: SseSource[A])(
      onConnect: (Client, SseSource[A]) => SseSource[A]): Route = {
    // FIXME: Is this escaped? How?
    path("events" / JavaUUID) { cuuid =>
      pathEnd {
        get {
          complete {
            val cid = Client(cuuid)
            onConnect(cid, source).keepAlive(10.second,
                                             () => ServerSentEvent.Heartbeat)
          }
        }
      }
    }
  }

  def queueUpdates[A: Encoder](event: HC.Event[Client => A], engine: HC.Engine)(
      client: Client,
      source: SseSource[SseQueue]): SseSource[SseQueue] = {
    source.mapMaterializedValue { queue =>
      val subscription = engine.subscribeForPulses { pulses =>
        val pulse = pulses(event)
        pulse.foreach { cf =>
          val messages = cf(client)
          val sse      = ServerSentEvent(messages.asJson.noSpaces, "updates")
          // FIXME: log failures
          queue.offer(sse)
        }
      }

      // FIXME: Does this end when HTTP SSE request ends?
      queue.watchCompletion().onComplete { _ =>
        subscription.cancel()
        println(s"FIXME: client $client disconnected")
      }

      queue
    }
  }

  def queueResets[A: Encoder](beh: HC.Behavior[Client => A],
                              engine: HC.Engine)(
      client: Client,
      source: SseSource[SseQueue]): SseSource[SseQueue] = {
    source.mapMaterializedValue { queue =>
      val currentValues = engine.askCurrentValues()
      val initials      = currentValues(beh)
      initials.foreach { cf =>
        val messages = cf(client)
        val sse      = ServerSentEvent(messages.asJson.noSpaces, "resets")
        // FIXME: log failures
        queue.offer(sse)
      }

      queue
    }
  }

  def exitRoute(exit: ExitData)(engine: HC.Engine): Route = {
    val queueSize = Int.MaxValue // FIXME: pick something sensible
    val src       = Source.queue[ServerSentEvent](queueSize, OverflowStrategy.fail)

    buildExitRoute(src) { (client, source) =>
      val src = queueResets(exit.behavior, engine)(client, source)
      queueUpdates(exit.event, engine)(client, src)
    }
  }

  //   def notify(change: ClientChange) =
  //     engine.fire(Seq(clientStatus -> change))

  //   def notifyClientHasConnected(cid: UUID) = notify(Connected(Client(cid)))
  //   def notifyClientHasDisconnected(cid: UUID) = notify(Disconnected(Client(cid)))

  // def inputRoute[A: Decoder](engine: HC.Engine)(eventSource: HC.EventSource[Client => Seq[A]]): Route = {
  //   get {
  //     path("inputEvent" / JavaUUID) { cid =>
  //       pathEnd {
  //         post {
  //           entity(as[Seq[A]]) { values =>
  //             val client = Client(cid)

  //             val clientData: Client => Seq[A] =
  //               Map(client -> values).withDefaultValue(Seq.empty)

  //             // Fire new value into the eventsource
  //             engine.fire(Seq(eventSource -> clientData))
  //             complete("OK")
  //           }
  //         }
  //       }
  //     }
  //   }
  // }
}
