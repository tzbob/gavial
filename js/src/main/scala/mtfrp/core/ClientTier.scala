package mtfrp
package core

import hokko.core
import hokko.core.tc
import io.circe.{Decoder, Encoder}

// Define all Client types
// Create all Client constructors

class ClientEvent[A] private[core] (rep: core.Event[A],
                                    graph: ReplicationGraph)
    extends HokkoEvent[ClientTier, A](rep, graph)

object ClientEvent extends HokkoEventObject {
  implicit val mtfrpCliEventInstances: tc.Event[ClientEvent, ClientIncBehavior] =
    this.makeInstances[ClientTier]

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
    rep: core.CBehavior[A],
    graph: ReplicationGraph
) extends HokkoBehavior[ClientTier, A](rep, graph)

object ClientBehavior extends HokkoBehaviorObject[ClientTier] {
  implicit val mtfrpBehaviorInstances = this.makeInstances
}

class ClientDiscreteBehavior[A] private[core] (
    rep: core.DBehavior[A],
    initial: A,
    graph: ReplicationGraph
) extends HokkoDiscreteBehavior[ClientTier, A](rep, initial, graph)

object ClientDiscreteBehavior extends HokkoDiscreteBehaviorObject[ClientTier] {
  implicit val mtfrpDBehaviorInstances = this.makeInstances
}

class ClientIncBehavior[A, DeltaA] private[core] (
    rep: core.IBehavior[A, DeltaA],
    initial: A,
    graph: ReplicationGraph,
    accumulator: (A, DeltaA) => A
) extends HokkoIncBehavior[ClientTier, A, DeltaA](rep,
                                                    initial,
                                                    graph,
                                                    accumulator)

object ClientIncBehavior extends HokkoIncrementalBehaviorObject {
  implicit val mtfrpDBehaviorInstances = this.makeInstances

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

final class ClientTier extends HokkoTier with ClientTierLike
