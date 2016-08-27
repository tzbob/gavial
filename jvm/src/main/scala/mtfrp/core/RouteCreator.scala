package mtfrp.core

import scala.concurrent.ExecutionContext.Implicits.global

import java.util.UUID
import scala.concurrent.duration._

import akka.http.scaladsl.server._
import akka.stream._
import akka.stream.scaladsl._
import de.heikoseeberger.akkasse._
import hokko.{core => HC}

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

object RouteCreator {
  import Directives._
  import EventStreamMarshalling._

  type SourceQueue[A] = SourceQueueWithComplete[A]

  def buildExitRoute[A](source: Source[Seq[Message], A])(
      onConnect: (Client, Source[Seq[Message], A]) => Source[Seq[Message], A])
    : Route = {
    // FIXME: Is this escaped? How?
    path("events" / JavaUUID) { cuuid =>
      pathEnd {
        get {
          complete {
            val cid = Client(cuuid)
            onConnect(cid, source)
              .map(messages => ServerSentEvent(messages.asJson.noSpaces))
              .keepAlive(10.second, () => ServerSentEvent.Heartbeat)
          }
        }
      }
    }
  }

  def queueEvent[A](event: HC.Event[Client => A], engine: HC.Engine)(
      client: Client,
      source: Source[A, SourceQueue[A]]): Source[A, SourceQueue[A]] = {
    source.mapMaterializedValue { (queue: SourceQueue[A]) =>
      val subscription = engine.subscribeForPulses { pulses =>
        val pulse = pulses(event)
        pulse.foreach { cf =>
          // FIXME: log failures
          queue.offer(cf(client))
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

  def exitRoute(exit: ExitData)(engine: HC.Engine): Route = {
    val queueSize = Int.MaxValue // FIXME: pick something sensible
    val src = Source.queue[Seq[Message]](queueSize, OverflowStrategy.fail)

    buildExitRoute(src)(queueEvent(exit.event, engine))
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
