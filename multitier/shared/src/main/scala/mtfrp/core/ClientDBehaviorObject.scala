package mtfrp.core

import io.circe.{Decoder, Encoder}
import slogging.LazyLogging

trait ClientDBehaviorObject extends LazyLogging {
  def toSession[A: Decoder: Encoder](
      db: ClientDBehavior[A]): SessionDBehavior[A] = {
    ClientIBehavior
      .toSession(db.toIBehavior((a, _) => a)((_, a) => a))
      .toDBehavior
  }
}
