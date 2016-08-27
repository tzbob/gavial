package mtfrp.core

import java.util.UUID

import cats.data.Xor
import hokko.{core => HC}
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import mtfrp.core.ReplicationGraph.Pulse
import org.scalajs.dom.raw._

class EventReceiver(graph: ReplicationGraph,
                    engine: HC.Engine,
                    listener: EventListener) {
  private[this] val pulseMakers = ReplicationGraph.inputEventRouter(graph)

  def decodeAsPulses(
      messages: String): Xor[Error, List[ReplicationGraph.Pulse]] = {
    val messageXor = decode[List[Message]](messages)
    messageXor.map { messages =>
      messages.map(pulseMakers).flatten // FIXME: Log when this discards things
    }
  }

  def restart(url: String): Unit = {
    def firePulses(pulsesXor: Xor[Error, List[Pulse]]): Unit = {
      pulsesXor.foreach(engine.fire) // FIXME: log when there is no xor.right
    }

    val decodeAndFire = (decodeAsPulses _).andThen(firePulses _)

    listener.restart(url,
                     Map(
                       "update" -> decodeAndFire,
                       "reset"  -> decodeAndFire
                     ))
  }
}
