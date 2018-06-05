package mtfrp.core

import io.circe._

trait ClientIBehaviorObject {
  def toSession[A: Decoder: Encoder, DeltaA: Decoder: Encoder](
      clientBeh: ClientIBehavior[A, DeltaA]): SessionIBehavior[A, DeltaA] = {
    val ib: AppIBehavior[Map[Client, A], Map[Client, DeltaA]] =
      ClientIBehavior.toApp(clientBeh).map { identity } {
        case (c, deltaA) => Map(c -> deltaA)
      } { (aMap, deltaMap) =>
        import cats.implicits._
        aMap ++ aMap.map2(deltaMap)(clientBeh.accumulator)
      }

    new SessionIBehavior(ib, clientBeh.initial, clientBeh.requiresWebSockets)
  }
}
