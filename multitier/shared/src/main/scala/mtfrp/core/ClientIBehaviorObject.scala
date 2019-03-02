package mtfrp.core

import cats.data.Ior
import io.circe._

trait ClientIBehaviorObject {
  def toSession[A: Decoder: Encoder, DeltaA: Decoder: Encoder](
      clientBeh: ClientIBehavior[A, DeltaA]): SessionIBehavior[A, DeltaA] = {
    val ib: AppIBehavior[Map[Client, A],
                         (Map[Client, DeltaA], Option[SessionChange[A]])] =
      ClientIBehavior
        .toApp(clientBeh)
        .map(identity) {
          case Ior.Both(pulse, change) =>
            Map(pulse) -> Option(
              SessionChange.fromClientChange(change, clientBeh.initial))
          case Ior.Left(pulse) =>
            Map(pulse) -> Option.empty[SessionChange[A]]
          case Ior.Right(change) =>
            Map.empty[Client, DeltaA] -> Option(
              SessionChange.fromClientChange(change, clientBeh.initial))
        }(IBehavior.transformFromNormalToSetClientChangeMapWithCurrent(
          clientBeh.accumulator))

    new SessionIBehavior(ib, clientBeh.initial, ib.graph)
  }
}
