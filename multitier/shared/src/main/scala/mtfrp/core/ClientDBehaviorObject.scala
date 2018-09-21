package mtfrp.core

import io.circe.{Decoder, Encoder}

trait ClientDBehaviorObject {
  def toSession[A: Decoder: Encoder](
      db: ClientDBehavior[A]): SessionDBehavior[A] = {
    ClientIBehavior
      .toSession(db.toIBehavior((_, a) => a)((_, a) => a))
      .toDBehavior
  }
}
