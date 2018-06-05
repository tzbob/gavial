package mtfrp.core

import io.circe.{Decoder, Encoder}
import mtfrp.core.mock.MockBuilder

object Replicator {

  def toClient[A, DeltaA](
      init: A,
      accumulator: (A, DeltaA) => A,
      state: AppBehavior[Client => A],
      deltas: AppEvent[Client => Option[DeltaA]]
  )(implicit da: Decoder[A],
    dda: Decoder[DeltaA],
    ea: Encoder[A],
    eda: Encoder[DeltaA]): ClientIBehavior[A, DeltaA] = {
    val mockBuilder = implicitly[MockBuilder[ClientTier]]
    val newGraph =
      ReplicationGraphServer.SenderBehavior(state.rep,
                                            deltas.rep,
                                            state.graph + deltas.graph)
    mockBuilder.IBehavior(newGraph, accumulator, init, true)
  }
}
