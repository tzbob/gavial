package mtfrp.core

import java.util.concurrent.TimeUnit

import akka.Done
import akka.http.scaladsl.model.ws
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream._
import akka.stream.scaladsl._
import hokko.core.Engine.{FireResult, Subscription}
import hokko.{core => HC}
import io.circe.generic.auto._
import io.circe.syntax._
import slogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

object WebSocketRouteCreator extends LazyLogging {
  def buildInputSinkGeneric(
      propagator: Seq[Message] => Unit): Sink[ws.Message, Future[Done]] = {
    Sink.foreach[ws.Message] {
      case ws.TextMessage.Strict(data) =>
        logger.debug(s"Received $data as a TextMessage")
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

class WebSocketRouteCreator(graph: ReplicationGraph) extends LazyLogging {
  private[this] val rgs      = new ReplicationGraphServer(graph)
  private[this] val exitData = rgs.exitData

  val engine: HC.Engine = HC.Engine.compile(exitData.event, exitData.behavior)

  def throwAllErrors(futures: Seq[Future[FireResult]]): Unit =
    futures.foreach {
      _.onComplete {
        case Failure(err)    => throw err
        case Success(result) => throwAllErrors(result.futurePropagations)
      }
    }

  def buildInputSink(client: Client): Sink[ws.Message, Any] =
    WebSocketRouteCreator
      .buildInputSinkGeneric { messages =>
        val pulses = messages.flatMap { msg =>
          inputRouter(client, msg)
        }
        logger.debug(s"Firing pulses: $pulses")
        throwAllErrors(engine.fire(pulses).futurePropagations)
      }
      .mapMaterializedValue { fut =>
        fut.onComplete {
          case Success(done) =>
            logger.debug(s"Input from Websocket is closed (success): ${done}")
            notifyClientHasDisconnected(client)
          case Failure(fail) =>
            logger.error(s"Input from Websocket is closed (failure): ${fail}")
            throw fail
        }
      }

  def buildExitSource(client: Client): Source[ws.Message, Unit] = {
    val queueSize = Int.MaxValue // FIXME: pick something sensible
    val src       = Source.queue[ws.Message](queueSize, OverflowStrategy.fail)
    src
      .keepAlive(FiniteDuration(1, TimeUnit.SECONDS),
                 () => TextMessage.Strict("hb"))
      .watchTermination() { (queue, done) =>
        val subscription = queueUpdates(client, queue)
        queueResets(client, queue)
        notifyClientHasConnected(client)

        done.onComplete { _ =>
          logger.debug(s"Output for Websocket is closed")
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
      logger.debug(s"Queuing update: $pulse")
      WebSocketRouteCreator.offer(client, queue, pulse)
    }
  }

  private[core] def queueResets(client: Client,
                                queue: SourceQueueWithComplete[ws.Message]) = {
    val currentValues = engine.askCurrentValues()
    val initials      = currentValues(exitData.behavior)
    logger.debug(s"Queuing reset: $initials")
    WebSocketRouteCreator.offer(client, queue, initials)
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

  def notify(change: ClientChange) = {
    logger.debug(s"Firing clientChange: $change")
    engine.fire(Seq(AppEvent.clientChangesSource -> change))
  }
  def notifyClientHasConnected(client: Client)    = notify(Connected(client))
  def notifyClientHasDisconnected(client: Client) = notify(Disconnected(client))
}
