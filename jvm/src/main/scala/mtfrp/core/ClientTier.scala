package mtfrp
package core

import hokko.core
import io.circe.{Decoder, Encoder}

// Define all Client types
// Create all Client constructors

class ClientEvent[A] private[core] (graph: ReplicationGraph)
    extends MockEvent[ClientTier, A](graph)

object ClientEvent {
  def apply[A](ev: core.Event[A]): ClientEvent[A] = empty
  def empty[A]: ClientEvent[A]                    = new ClientEvent(ReplicationGraph.start)

  implicit class ToAppEvent[A: Decoder: Encoder](clientEv: ClientEvent[A]) {
    def toApp(): AppEvent[(Client, A)] = {
      val hokkoBuilder  = implicitly[HokkoBuilder[AppTier]]
      val receiverGraph = ReplicationGraphServer.ReceiverEvent(clientEv.graph)
      hokkoBuilder.event(receiverGraph.source, receiverGraph)
    }
  }
}

class ClientBehavior[A] private[core] (graph: ReplicationGraph)
    extends MockBehavior[ClientTier, A](graph)

class ClientDiscreteBehavior[A] private[core] (
    graph: ReplicationGraph,
    initial: A
) extends MockDiscreteBehavior[ClientTier, A](graph, initial)

class ClientIncBehavior[A, DeltaA] private[core] (
    graph: ReplicationGraph,
    accumulator: (A, DeltaA) => A,
    initial: A
) extends MockIncBehavior[ClientTier, A, DeltaA](graph, accumulator, initial)

object ClientIncBehavior {
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

      val behavior = newGraph.deltas.fold(defaultValue)(transformed)

      hokkoBuilder
        .incrementalBehavior(behavior, defaultValue, newGraph, transformed)
    }
  }
}

final class ClientTier extends MockTier with ClientTierLike {
  type T = ClientTier
}

object ClientBehavior extends MockBehaviorOps[ClientTier]
object ClientDiscreteBehavior extends MockDiscreteBehaviorOps[ClientTier]
