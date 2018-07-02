package mtfrp.core

import akka.http.scaladsl.server.Directives._
import hokko.core.Engine
import hokko.{core => HC}
import io.circe
import io.circe.generic.auto._
import io.circe.syntax._
import slogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class XhrRouteCreator(graph: ReplicationGraph) extends LazyLogging {
  private[this] val rgs         = new ReplicationGraphServer(graph)
  private[this] val exitData    = rgs.exitData
  private[this] val inputRouter = rgs.inputEventRouter

  val engine: HC.Engine = HC.Engine.compile(exitData.event, exitData.behavior)

  def decodeData(data: String): Either[circe.Error, Seq[Message]] = {
    io.circe.parser.decode[Seq[Message]](data)
  }

  def fireMessages(client: Client,
                   messages: Seq[Message]): Engine.FireResult = {
    val pulses = messages.flatMap { message =>
      inputRouter(client, message)
    }
    logger.debug(s"Firing $pulses for $client")
    engine.fire(Seq(AppEvent.clientChangesSource -> Connected(client)))
    val pulseResults = engine.fire(pulses)
    engine.fire(Seq(AppEvent.clientChangesSource -> Disconnected(client)))
    logger.debug(s"$pulseResults as result for firing $pulses")
    pulseResults
  }

  def retrievePulse(client: Client,
                    fireResult: Engine.FireResult): Option[List[Message]] = {
    val exitDataOpt: Option[Client => Seq[Message]] =
      fireResult.Pulses(exitData.event)
    exitDataOpt.map(_(client).toList)
  }

  def handleRequest(client: Client,
                    stringData: String): Either[Exception, Seq[Message]] = {

    val messages      = decodeData(stringData)
    val fireResultErr = messages.map(msgs => fireMessages(client, msgs))

    fireResultErr.flatMap { fireResult: Engine.FireResult =>
      val eventualResults: List[Future[Engine.FireResult]] =
        fireResult.futurePropagations.toList
      val results: List[Engine.FireResult] =
        Await.result(Future.sequence(eventualResults), Duration(10, "s"))

      val pulses: List[Option[List[Message]]] =
        (fireResult +: results).map { result =>
          retrievePulse(client, result)
        }

      logger.debug(s"Awaited pulse: $pulses")

      val flatPulses = pulses.collect { case Some(msgs) => msgs }.flatten

      if (flatPulses.isEmpty)
        Left(new RuntimeException("No pulses as result."))
      else
        Right(flatPulses)
    }
  }

  val route = path(Names.xhr / JavaUUID) { cuuid =>
    pathEnd {
      post {
        entity(as[String]) { data =>
          complete {
            logger.debug(s"Handling request for $cuuid with $data")
            handleRequest(Client(cuuid), data) match {
              case Right(messages) => messages.asJson.noSpaces
              case Left(exception) => exception.getMessage
            }
          }
        }
      }
    }
  }
}
