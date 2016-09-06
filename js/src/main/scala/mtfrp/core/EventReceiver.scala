package mtfrp.core

import cats.data.Xor
import hokko.{core => HC}
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._

class EventReceiver(rgc: ReplicationGraphClient,
                    engine: HC.Engine,
                    listener: EventListener) {
  private[this] val pulseMakers = rgc.inputEventRouter

  def decodeAsPulses(
      messages: String): Xor[Error, List[ReplicationGraph.Pulse]] = {
    val messageXor = decode[List[Message]](messages)
    messageXor.map { messages =>
      val maybePulses = messages.map(pulseMakers)
      maybePulses.flatten // FIXME: Log when this discards things
    }
  }

  def restart(url: String): Unit = {
    val decodeAndFire = { (str: String) =>
      val decoded = decodeAsPulses(str)
      decoded.foreach(engine.fire) // FIXME: log when there is no xor.right
    }

    listener.restart(url,
                     Map(
                       "update" -> decodeAndFire,
                       "reset"  -> decodeAndFire
                     ))
  }
}
