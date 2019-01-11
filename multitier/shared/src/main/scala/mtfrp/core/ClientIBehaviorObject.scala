package mtfrp.core

import cats.data.Ior
import io.circe._

trait ClientIBehaviorObject {
  def toSession[A: Decoder: Encoder, DeltaA: Decoder: Encoder](
      clientBeh: ClientIBehavior[A, DeltaA]): SessionIBehavior[A, DeltaA] = {
    val ib
      : AppIBehavior[Map[Client, A], (Map[Client, DeltaA], Set[ClientChange])] =
      ClientIBehavior
        .toApp(clientBeh)
        .map(identity) {
          case Ior.Both(pulse, change) => Map(pulse) -> Set(change)
          case Ior.Left(pulse)         => Map(pulse) -> Set.empty[ClientChange]
          case Ior.Right(change) =>
            Map.empty[Client, DeltaA] -> Set(change)
        }(
          IBehavior.transformFromNormalToSetClientChangeMap(
            clientBeh.initial,
            clientBeh.accumulator))

    new SessionIBehavior(ib, clientBeh.initial, ib.graph)
  }
}
