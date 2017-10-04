package mtfrp.core

import hokko.{core => HC}
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalajs.dom
import org.scalajs.dom.WebSocket

class EventSender(rgc: ReplicationGraphClient,
                  engine: HC.Engine,
                  ws: WebSocket) {
  def start(): Unit = {
    require(ws.readyState == WebSocket.OPEN)
    engine.subscribeForPulses { pulses =>
      val pulse = pulses(rgc.exitEvent)
      pulse.foreach { messages =>
        val jsonString = messages.asJson.noSpaces
        ws.send(jsonString)
      }
    }
    ()
  }
}
