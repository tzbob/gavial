package mtfrp.core

import hokko.{core => HC}
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalajs.dom

class EventSender(rgc: ReplicationGraphClient, engine: HC.Engine) {
  def start(url: String): Unit = {
    engine.subscribeForPulses { pulses =>
      val pulse = pulses(rgc.exitEvent)
      pulse.foreach { messages =>
        val jsonString = messages.asJson.noSpaces
        val xhr        = new dom.XMLHttpRequest()
        xhr.open("POST", url)
        xhr.setRequestHeader("Content-Type", "application/json")
        xhr.send(jsonString)
      }
    }
    ()
  }
}
