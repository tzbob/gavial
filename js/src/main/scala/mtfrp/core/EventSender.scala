package mtfrp.core

import hokko.{core => HC}
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalajs.dom
import org.scalajs.dom.WebSocket
import slogging.LazyLogging

class EventSender(rgc: ReplicationGraphClient,
                  engine: HC.Engine,
                  ws: WebSocket) extends LazyLogging {
  def start(): Unit = {
    require(ws.readyState == WebSocket.OPEN)
    engine.subscribeForPulses { pulses =>
      val pulse = pulses(rgc.exitEvent)
      pulse.foreach { messages =>
        val jsonString = messages.asJson.noSpaces
        logger.debug(s"Sending $jsonString over WebSocket")
        ws.send(jsonString)
      }
    }
    ()
  }
}
