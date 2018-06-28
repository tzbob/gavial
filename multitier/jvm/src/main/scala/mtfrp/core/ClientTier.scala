package mtfrp
package core

import io.circe.{Decoder, Encoder}
import mtfrp.core.impl.HokkoBuilder
import mtfrp.core.mock._

class ClientEvent[A] private[core] (graph: GraphState)
    extends MockEvent[ClientTier, A](graph)

class ClientEventSource[A] private[core] (
    graph: GraphState
) extends ClientEvent[A](graph)

object ClientEvent extends MockEventObject[ClientTier] with ClientEventObject {
  def source[A]: ClientEventSource[A] =
    new ClientEventSource(GraphState.default)

  private[core] def toAppWithClient[A: Decoder: Encoder](
      clientEv: ClientEvent[A]): AppEvent[(Client, A)] = {
    val hokkoBuilder = implicitly[HokkoBuilder[AppTier]]
    val receiverGraph =
      ReplicationGraphServer.ReceiverEvent(clientEv.graph.replicationGraph)
    hokkoBuilder.event(receiverGraph.source,
                       GraphState(true, receiverGraph, _ => ()))
  }

}

class ClientBehavior[A] private[core] (graph: GraphState)
    extends MockBehavior[ClientTier, A](graph)

class ClientBehaviorSink[A] private[core] (graph: GraphState)
    extends ClientBehavior[A](graph)

object ClientBehavior extends MockBehaviorObject[ClientTier] {
  def sink[A](default: A): ClientBehaviorSink[A] = new ClientBehaviorSink(
    GraphState.default
  )
}

class ClientDBehavior[A] private[core] (
    graph: GraphState,
    initial: A
) extends MockDBehavior[ClientTier, A](graph, initial)

object ClientDBehavior extends MockDBehaviorObject[ClientTier]

class ClientIBehavior[A, DeltaA] private[core] (
    graph: GraphState,
    accumulator: (A, DeltaA) => A,
    initial: A
) extends MockIBehavior[ClientTier, A, DeltaA](graph, accumulator, initial)

object ClientIBehavior extends MockIBehaviorObject with ClientIBehaviorObject {
  def toApp[A: Decoder: Encoder, DeltaA: Decoder: Encoder](
      clientBeh: ClientIBehavior[A, DeltaA])
    : AppIBehavior[Map[Client, A], (Client, DeltaA)] = {
    val hokkoBuilder = implicitly[HokkoBuilder[AppTier]]

    val newGraph =
      ReplicationGraphServer.ReceiverBehavior[A, DeltaA](
        clientBeh.graph.replicationGraph)
    val state = GraphState(true, newGraph, _ => ())

    val transformed =
      IBehavior.transformFromNormal(clientBeh.accumulator)
    val defaultValue =
      Map.empty[Client, A].withDefaultValue(clientBeh.initial)

    val deltas   = newGraph.deltas.source
    val behavior = deltas.fold(defaultValue)(transformed)

    hokkoBuilder.IBehavior(behavior, defaultValue, state, transformed)
  }
}

object ClientAsync extends MockAsync[ClientTier]

final class ClientTier extends MockTier with ClientTierLike