package mtfrp.core

import io.circe._

trait ClientEventObject {
  def toSession[A: Decoder: Encoder](
      clientEv: ClientEvent[A]): SessionEvent[A] =
    new SessionEvent(ClientEvent.toAppWithClient(clientEv).map {
      case (c, a) => Map(c -> a)
    }, true)

  def toApp[A: Decoder: Encoder](clientEv: ClientEvent[A]): AppEvent[A] =
    ClientEvent.toAppWithClient(clientEv).map {
      case (_, a) => a
    }
}
