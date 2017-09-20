package mtfrp
package core

import io.circe.{Decoder, Encoder}
import mtfrp.core.impl.HokkoBuilder
import mtfrp.core.mock._

// Define all Client types
// Create all Client constructors

class ClientEvent[A] private[core] (graph: ReplicationGraph)
    extends MockEvent[ClientTier, A](graph)

class ClientEventSource[A] private[core] (
    graph: ReplicationGraph
) extends ClientEvent[A](graph)

object ClientEvent extends MockEventObject[ClientTier] {
  def source[A]: ClientEventSource[A] =
    new ClientEventSource(ReplicationGraph.start)

  def toApp[A: Decoder: Encoder](clientEv: ClientEvent[A]) = {
    val hokkoBuilder  = implicitly[HokkoBuilder[AppTier]]
    val receiverGraph = ReplicationGraphServer.ReceiverEvent(clientEv.graph)
    hokkoBuilder.event(receiverGraph.source, receiverGraph)
  }
}

class ClientBehavior[A] private[core] (graph: ReplicationGraph)
    extends MockBehavior[ClientTier, A](graph)

class ClientBehaviorSink[A] private[core] (graph: ReplicationGraph)
    extends ClientBehavior[A](graph)

object ClientBehavior extends MockBehaviorObject[ClientTier] {
  def sink[A](default: A): ClientBehaviorSink[A] = new ClientBehaviorSink(
    ReplicationGraph.start
  )
}

class ClientDiscreteBehavior[A] private[core] (
    graph: ReplicationGraph,
    initial: A
) extends MockDiscreteBehavior[ClientTier, A](graph, initial)

object ClientDiscreteBehavior extends MockDiscreteBehaviorObject[ClientTier]

class ClientIncBehavior[A, DeltaA] private[core] (
    graph: ReplicationGraph,
    accumulator: (A, DeltaA) => A,
    initial: A
) extends MockIncBehavior[ClientTier, A, DeltaA](graph, accumulator, initial)

object ClientIncBehavior extends MockIncrementalBehaviorObject {
  def toApp[A: Decoder: Encoder, DeltaA: Decoder: Encoder](
      clientBeh: ClientIncBehavior[A, DeltaA]) = {
    val hokkoBuilder = implicitly[HokkoBuilder[AppTier]]

    val newGraph =
      ReplicationGraphServer.ReceiverBehavior[A, DeltaA](clientBeh.graph)

    val transformed =
      IncrementalBehavior.transformFromNormal(clientBeh.accumulator)
    // FIXME: relying on Map.default is dangerous
    val defaultValue =
      Map.empty[Client, A].withDefaultValue(clientBeh.initial)

    val deltas   = newGraph.deltas.source
    val behavior = deltas.fold(defaultValue)(transformed)

    hokkoBuilder
      .incrementalBehavior(behavior, defaultValue, newGraph, transformed)
  }
}

final class ClientTier extends MockTier with ClientTierLike
