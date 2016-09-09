package mtfrp
package core

import io.circe.{Decoder, Encoder}

// Define all Client types
// Create all Client constructors

class ClientEvent[A] private[core] (graph: ReplicationGraph)
    extends MockEvent[ClientTier, A](graph)

object ClientEvent extends MockEventObject[ClientTier] {
  implicit class ToAppEvent[A: Decoder: Encoder](clientEv: ClientEvent[A]) {
    def toApp(): AppEvent[(Client, A)] = {
      val hokkoBuilder  = implicitly[HokkoBuilder[AppTier]]
      val receiverGraph = ReplicationGraphServer.ReceiverEvent(clientEv.graph)
      hokkoBuilder.event(receiverGraph.source.toEvent, receiverGraph)
    }
  }
}

class ClientBehavior[A] private[core] (graph: ReplicationGraph)
    extends MockBehavior[ClientTier, A](graph)

object ClientBehavior extends MockBehaviorObject[ClientTier]

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
  implicit class ToServerBehavior[A: Decoder: Encoder,
  DeltaA: Decoder: Encoder](clientBeh: ClientIncBehavior[A, DeltaA]) {
    def toApp(): AppIncBehavior[Map[Client, A], (Client, DeltaA)] = {
      val hokkoBuilder = implicitly[HokkoBuilder[AppTier]]

      val newGraph =
        ReplicationGraphServer.ReceiverBehavior(clientBeh.graph)

      val transformed =
        IncrementalBehavior.transformFromNormal(clientBeh.accumulator)
      // FIXME: relying on Map.default is dangerous
      val defaultValue =
        Map.empty[Client, A].withDefaultValue(clientBeh.initial)

      val behavior = newGraph.deltas.toEvent.fold(defaultValue)(transformed)

      hokkoBuilder
        .incrementalBehavior(behavior, defaultValue, newGraph, transformed)
    }
  }
}

final class ClientTier extends MockTier with ClientTierLike
