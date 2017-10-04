package mtfrp.core

import akka.Done
import akka.http.scaladsl.model.ws
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream._
import akka.stream.scaladsl._
import hokko.core.Engine.Subscription
import hokko.{core => HC}
import io.circe.generic.auto._
import io.circe.syntax._
import slogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object RouteCreator extends LazyLogging {
  def buildInputSinkGeneric(
      propagator: Seq[Message] => Unit): Sink[ws.Message, Future[Done]] = {
    Sink.foreach[ws.Message] {
      case ws.TextMessage.Strict(data) =>
        val messagesEither = io.circe.parser.decode[Seq[Message]](data)
        messagesEither match {
          case Left(err)       => logger.error(err.toString)
          case Right(messages) => propagator(messages)
        }
      case x => logger.error(s"Received something other than a TextMessage: $x")
    }
  }

  def offer(client: Client,
            queue: SourceQueueWithComplete[ws.Message],
            pulse: Option[Client => Seq[Message]]): Unit = {
    pulse match {
      case Some(cf) =>
        val messages = cf(client)
        val msg      = TextMessage.Strict(messages.asJson.noSpaces)
        val qOffer   = queue.offer(msg)
        qOffer.failed.foreach { t =>
          logger.info(s"Could not offer $msg to $queue: $t")
        }
      case _ => logger.info(s"No pulse created")
    }
  }
}

class RouteCreator(graph: ReplicationGraph) extends LazyLogging {
  private[this] val rgs      = new ReplicationGraphServer(graph)
  private[this] val exitData = rgs.exitData

  private[this] val clientChangesSource = HC.Event.source[ClientChange]
  val clientChanges: AppEvent[ClientChange] =
    new AppEvent(clientChangesSource, ReplicationGraph.start)
  val engine: HC.Engine = HC.Engine.compile(exitData.event, clientChangesSource)

  def buildInputSink(client: Client): Sink[ws.Message, Future[Done]] =
    RouteCreator.buildInputSinkGeneric { messages =>
      val pulses = messages.flatMap { msg =>
        inputRouter(client, msg)
      }
      engine.fire(pulses)
    }

  def buildExitSource(client: Client): Source[ws.Message, Unit] = {
    val queueSize = Int.MaxValue // FIXME: pick something sensible
    val src       = Source.queue[ws.Message](queueSize, OverflowStrategy.fail)
    src.watchTermination() { (queue, done) =>
      notifyClientHasConnected(client)

      queueResets(client, queue)
      val subscription = queueUpdates(client, queue)

      done.onComplete { _ =>
        subscription.cancel()
        notifyClientHasDisconnected(client)
      }
    }
  }

  private[core] def queueUpdates(
      client: Client,
      queue: SourceQueueWithComplete[ws.Message]): Subscription = {
    engine.subscribeForPulses { pulses =>
      val pulse = pulses(exitData.event)
      RouteCreator.offer(client, queue, pulse)
    }
  }

  private[core] def queueResets(client: Client,
                                queue: SourceQueueWithComplete[ws.Message]) = {
    val currentValues = engine.askCurrentValues()
    val initials      = currentValues(exitData.behavior)
    RouteCreator.offer(client, queue, initials)
  }

  def buildRoute(flow: Client => Flow[ws.Message, ws.Message, Any]): Route =
    path(Names.ws / JavaUUID) { cuuid =>
      val cid = Client(cuuid)
      pathEnd {
        get {
          handleWebSocketMessages(flow(cid))
        }
      }
    }

  val route: Route = buildRoute { cid =>
    Flow.fromSinkAndSource(buildInputSink(cid), buildExitSource(cid))
  }

  private[this] val inputRouter = rgs.inputEventRouter

  def notify(change: ClientChange) =
    engine.fire(Seq(clientChangesSource -> change))
  def notifyClientHasConnected(client: Client)    = notify(Connected(client))
  def notifyClientHasDisconnected(client: Client) = notify(Disconnected(client))
}
