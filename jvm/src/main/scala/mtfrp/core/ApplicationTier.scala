package mtfrp
package core

import hokko.core
import io.circe._

// Define all Application types
// Create all Application constructors

class ApplicationEvent[A] private[core] (
  rep: core.Event[A],
  graph: ReplicationGraph
) extends HokkoEvent[ApplicationTier, A](rep, graph)

object ApplicationEvent {
  implicit class ReplicableEvent[A](appEv: ApplicationEvent[Client => Option[A]]) {
    def toClient(implicit da: Decoder[A], ea: Encoder[A]): ClientEvent[A] = {
      val mockBuilder = implicitly[MockBuilder[ClientTier]]
      val source = core.Event.source[A]
      val newGraph = ReplicationGraph.eventSender(appEv.graph, appEv.rep)
      mockBuilder.event(newGraph)
    }
  }
}

class ApplicationBehavior[A] private[core] (
  rep: core.Behavior[A],
  graph: ReplicationGraph
) extends HokkoBehavior[ApplicationTier, A](rep, graph)

class ApplicationDiscreteBehavior[A] private[core] (
  rep: core.DiscreteBehavior[A],
  initial: A,
  graph: ReplicationGraph
) extends HokkoDiscreteBehavior[ApplicationTier, A](rep, initial, graph)

class ApplicationIncBehavior[A, DeltaA] private[core] (
  rep: core.IncrementalBehavior[A, DeltaA],
  initial: A,
  graph: ReplicationGraph
) extends HokkoIncBehavior[ApplicationTier, A, DeltaA](rep, initial, graph)

object ApplicationIncBehavior {
  implicit class ReplicableIBehavior[A, DeltaA](appBeh: ApplicationIncBehavior[Client => A, Client => Option[DeltaA]]) {
    def toClient(implicit da: Decoder[A], dda: Decoder[DeltaA], ea: Encoder[A], eda: Encoder[DeltaA]): ClientIncBehavior[A, DeltaA] = {
      val mockBuilder = implicitly[MockBuilder[ClientTier]]
      val newGraph = ReplicationGraph.behaviorSender(appBeh.graph, appBeh.rep)
      // FIXME: ??? is bootstrapping
      mockBuilder.incrementalBehavior(newGraph, ???)
    }
  }
}

final class ApplicationTier extends HokkoTier with ApplicationTierLike {
  type T = ApplicationTier
}

object ApplicationBehavior extends HokkoBehaviorOps[ApplicationTier]
object ApplicationDiscreteBehavior extends HokkoDiscreteBehaviorOps[ApplicationTier]
