package mtfrp.core

import io.circe.{Decoder, Encoder}
import hokko.core
import mtfrp.core.impl.HokkoBuilder

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

    val hokkoBuilder = implicitly[HokkoBuilder[ClientTier]]

    val newGraph =
      ReplicationGraphClient.ReceiverBehavior[A, DeltaA](
        state.graph.replicationGraph + deltas.graph.replicationGraph)

    val deltaSource = newGraph.deltas.source
    val resets      = newGraph.resets

    // TODO: Improve with an initial value reader/injector
    /*
    This is correct, the actual initial values are sent as a
    reset request, the initial values of behaviors are only used as
    an asap-initialisation mechanism.
     */

    val replicatedBehavior: core.IBehavior[A, DeltaA] =
      deltaSource.resetFold(resets)(init) { (acc, n) =>
        accumulator(acc, n)
      }

    hokkoBuilder.IBehavior(
      replicatedBehavior,
      state.graph.mergeGraphAndEffect(deltas.graph).ws.withGraph(newGraph),
    )
  }
}
