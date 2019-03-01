package mtfrp.core

import hokko.{core => HC}
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalajs.dom.{WebSocket, XMLHttpRequest}
import org.scalajs.dom.ext.Ajax
import slogging.LazyLogging
import scala.concurrent.ExecutionContext.Implicits.global

class XhrEventSender(rgc: ReplicationGraphClient, engine: HC.Engine)
    extends LazyLogging {

  def start(url: String): Unit = {
    // FIXME: Poll here for initial values
    engine.subscribeForPulses { pulses =>
      val pulse = pulses(rgc.exitEvent)
      pulse.foreach { messages =>
        val jsonString = messages.asJson.noSpaces
        logger.debug(s"Sending $jsonString on $url")

        val eventualRequest = Ajax.post(url, jsonString)
        eventualRequest.map { xhr: XMLHttpRequest =>
          val data   = xhr.responseText
          val pulses = EventReceiver.decodeAsPulses(rgc, data)

          pulses match {
            case Right(pulses) => engine.fire(pulses)
            case Left(err) =>
              logger.error(
                s"Error decoding xhr result from ${xhr.responseURL}: $err")
          }
        }
      }
    }

  }
}
