package mtfrp.core

import io.circe.{Decoder, Encoder}
import mtfrp.core.mock.MockBuilder

import cats.implicits._

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
      state.graph.replicationGraph.map2(deltas.graph.replicationGraph) {
        (srg, drg) =>
          ReplicationGraphServer.SenderBehavior(state.rep,
                                                deltas.rep,
                                                srg + drg)
      }
    mockBuilder.IBehavior(state.graph
                            .mergeGraphAndEffect(deltas.graph)
                            .ws
                            .withGraph(newGraph),
                          accumulator,
                          init)
  }
}
