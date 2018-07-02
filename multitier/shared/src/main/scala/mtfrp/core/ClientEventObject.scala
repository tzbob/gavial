package mtfrp.core

import hokko.core.Engine
import io.circe._

trait ClientEventObject {
  def toSession[A: Decoder: Encoder](
      clientEv: ClientEvent[A]): SessionEvent[A] = {
    val value =
      ClientEvent.toAppWithClient(clientEv).map { case (c, a) => Map(c -> a) }
    new SessionEvent(value, value.graph.ws)
  }

  def toApp[A: Decoder: Encoder](clientEv: ClientEvent[A]): AppEvent[A] =
    ClientEvent.toAppWithClient(clientEv).map {
      case (_, a) => a
    }

}
