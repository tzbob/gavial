package mtfrp.core

import hokko.{core => HC}
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import slogging.LazyLogging

class EventReceiver(rgc: ReplicationGraphClient,
                    engine: HC.Engine,
                    listener: EventListener)
    extends LazyLogging {
  private[this] val pulseMakers = rgc.inputEventRouter

  def decodeAsPulses(
      messages: String): Either[Error, List[ReplicationGraph.Pulse]] = {
    val messageXor = decode[List[Message]](messages)
    messageXor.right.map { messages =>
      val maybePulses = messages.map { message =>
        val maybePulse = pulseMakers(message)
        if (maybePulse.isEmpty)
          logger.info(s"Pulse not created for msg: $message")
        maybePulse
      }
      maybePulses.flatten
    }
  }

  def restart(url: String): Unit = {
    val decodeAndFire = { (str: String) =>
      val decoded = decodeAsPulses(str)
      decoded match {
        case Right(pulses) =>
          logger.debug(s"Firing pulses: $pulses")
          engine.fire(pulses)
          ()
        case Left(err) =>
          logger.info(s"Could not decode: $str, error: $err")
      }
    }

    listener.restart(url, decodeAndFire)
  }
}
