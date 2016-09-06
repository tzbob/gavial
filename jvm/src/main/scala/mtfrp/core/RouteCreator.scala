package mtfrp.core

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, _}
import akka.stream._
import akka.stream.scaladsl._
import de.heikoseeberger.akkahttpcirce.CirceSupport
import de.heikoseeberger.akkasse._
import hokko.core.Engine
import hokko.{core => HC}
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object RouteCreator {
  import CirceSupport._
  import Directives._
  import EventStreamMarshalling._

  type SourceQueue[A] = SourceQueueWithComplete[A]
  type SseQueue       = SourceQueue[ServerSentEvent]
  type SseSource[A]   = Source[ServerSentEvent, A]

  def buildInputRoute[A: Decoder](onPost: (Client, A) => Unit): Route = {
    path(Names.toServerUpdates / JavaUUID) { cuuid =>
      pathEnd {
        post {
          entity(as[A]) { values =>
            val cid = Client(cuuid)
            onPost(cid, values)
            // TODO: return result in the same HttpRequest (Optimization)!
            complete("OK")
          }
        }
      }
    }
  }

  def buildExitRoute[A](source: SseSource[A])(
      onConnect: (Client, SseSource[A]) => SseSource[A]): Route = {
    path(Names.toClientUpdates / JavaUUID) { cuuid =>
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

  def queueUpdates[A: Encoder](event: HC.Event[Client => A],
                               engine: HC.Engine)(
      client: Client,
      source: SseSource[SseQueue]): SseSource[SseQueue] = {
    source.mapMaterializedValue { queue =>
      val subscription = engine.subscribeForPulses { pulses =>
        val pulse = pulses(event)
        pulse.foreach { cf =>
          val messages = cf(client)
          val sse      = ServerSentEvent(messages.asJson.noSpaces, Names.Sse.update)
          // FIXME: log failures
          queue.offer(sse)
        }
      }

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
        val sse      = ServerSentEvent(messages.asJson.noSpaces, Names.Sse.reset)
        // FIXME: log failures
        queue.offer(sse)
      }

      queue
    }
  }
}

class RouteCreator(graph: ReplicationGraph) {
  private[this] val rgs      = new ReplicationGraphServer(graph)
  private[this] val exitData = rgs.exitData
  val engine: Engine         = HC.Engine.compile(Seq(exitData.event), Nil)

  val exitRoute: Route = {
    val queueSize = Int.MaxValue // FIXME: pick something sensible
    val src       = Source.queue[ServerSentEvent](queueSize, OverflowStrategy.fail)

    RouteCreator.buildExitRoute(src) { (client, source) =>
      val srcFromResets =
        RouteCreator.queueResets(exitData.behavior, engine)(client, source)
      RouteCreator.queueUpdates(exitData.event, engine)(client, srcFromResets)
    }
  }

  private[this] val inputRouter = rgs.inputEventRouter

  val inputRoute: Route = {
    RouteCreator.buildInputRoute { (client: Client, messages: Seq[Message]) =>
      val pulses = messages.flatMap { msg =>
        inputRouter(client, msg)
      }
      engine.fire(pulses)
    }
  }

  val route: Route = exitRoute ~ inputRoute

  //   def notify(change: ClientChange) =
  //     engine.fire(Seq(clientStatus -> change))

  //   def notifyClientHasConnected(cid: UUID) = notify(Connected(Client(cid)))
  //   def notifyClientHasDisconnected(cid: UUID) = notify(Disconnected(Client(cid)))

}
