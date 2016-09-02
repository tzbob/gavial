package mtfrp
package core

import hokko.core
import io.circe.{Decoder, Encoder}

// Define all Client types
// Create all Client constructors

class ClientEvent[A] private[core] (
    rep: core.Event[A],
    graph: ReplicationGraph
) extends HokkoEvent[ClientTier, A](rep, graph)

object ClientEvent {
  def apply[A](ev: core.Event[A]): ClientEvent[A] =
    new ClientEvent(ev, ReplicationGraph.start)

  def empty[A]: ClientEvent[A] =
    new ClientEvent(core.Event.source[A], ReplicationGraph.start)

  implicit class ToAppEvent[A: Decoder: Encoder](clientEv: ClientEvent[A]) {
    def toApp(): AppEvent[(Client, A)] = {
      val mockBuilder = implicitly[MockBuilder[AppTier]]
      val newGraph =
        ReplicationGraphClient.SenderEvent(clientEv.rep, clientEv.graph)
      mockBuilder.event(newGraph)
    }
  }
}

class ClientBehavior[A] private[core] (
    rep: core.Behavior[A],
    graph: ReplicationGraph
) extends HokkoBehavior[ClientTier, A](rep, graph)

class ClientDiscreteBehavior[A] private[core] (
    rep: core.DiscreteBehavior[A],
    initial: A,
    graph: ReplicationGraph
) extends HokkoDiscreteBehavior[ClientTier, A](rep, initial, graph)

class ClientIncBehavior[A, DeltaA] private[core] (
    rep: core.IncrementalBehavior[A, DeltaA],
    initial: A,
    graph: ReplicationGraph,
    accumulator: (A, DeltaA) => A
) extends HokkoIncBehavior[ClientTier, A, DeltaA](rep,
                                                    initial,
                                                    graph,
                                                    accumulator)
object ClientIncBehavior {
  implicit class ToServerBehavior[A: Decoder: Encoder,
  DeltaA: Decoder: Encoder](clientBeh: ClientIncBehavior[A, DeltaA]) {
    def toApp(): AppIncBehavior[Map[Client, A], (Client, DeltaA)] = {
      val mockBuilder = implicitly[MockBuilder[AppTier]]

      val newGraph =
        ReplicationGraphClient.SenderBehavior(clientBeh.rep, clientBeh.graph)

      val transformed =
        IncrementalBehavior.transformFromNormal(clientBeh.accumulator)
      // FIXME: relying on Map.default is dangerous
      val defaultValue =
        Map.empty[Client, A].withDefaultValue(clientBeh.initial)

      mockBuilder.incrementalBehavior(newGraph, transformed, defaultValue)
    }
  }
}

final class ClientTier extends HokkoTier with ClientTierLike {
  type T = ClientTier
}

object ClientBehavior extends HokkoBehaviorOps[ClientTier]
object ClientDiscreteBehavior extends HokkoDiscreteBehaviorOps[ClientTier]
