package mtfrp.core

import io.circe._

trait ClientEventObject {
  def toSession[A: Decoder: Encoder](
      clientEv: ClientEvent[A]): SessionEvent[A] =
    new SessionEvent(ClientEvent.toApp(clientEv).map {
      case (c, a) => Map(c -> a)
    })
}
