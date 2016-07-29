package mtfrp
package core

import hokko.core
import io.circe._

// Define all Application types
// Create all Application constructors

class ApplicationEvent[A] private[core] (graph: ReplicationGraph)
  extends MockEvent[ApplicationTier, A](graph)

object ApplicationEvent {
  implicit class ReplicableEvent[A](appEv: ApplicationEvent[Client => Option[A]]) {
    def toClient(implicit da: Decoder[A], ea: Encoder[A]): ClientEvent[A] = {
      val hokkoBuilder = implicitly[HokkoBuilder[ClientTier]]
      val source = core.Event.source[A]
      val newGraph = ReplicationGraph.eventReceiver(appEv.graph, source)
      hokkoBuilder.event(source, newGraph)
    }
  }
}

class ApplicationBehavior[A] private[core] (graph: ReplicationGraph)
  extends MockBehavior[ApplicationTier, A](graph)

class ApplicationDiscreteBehavior[A] private[core] (
  graph: ReplicationGraph,
  init: A
) extends MockDiscreteBehavior[ApplicationTier, A](init, graph)

class ApplicationIncBehavior[A, DeltaA] private[core] (
  graph: ReplicationGraph,
  init: A
) extends MockIncBehavior[ApplicationTier, A, DeltaA](init, graph)

object ApplicationIncBehavior {
  implicit class ReplicableIBehavior[A, DeltaA](appBeh: ApplicationIncBehavior[Client => A, Client => Option[DeltaA]]) {
    def toClient(implicit da: Decoder[A], dda: Decoder[DeltaA], ea: Encoder[A], eda: Encoder[DeltaA]): ClientIncBehavior[A, DeltaA] = {
      val hokkoBuilder = implicitly[HokkoBuilder[ClientTier]]
      val source = core.Event.source[DeltaA]
      val newGraph = ReplicationGraph.behaviorReceiver(appBeh.graph, source)
      // FIXME: ??? is bootstrapping
      hokkoBuilder.incrementalBehavior(???, ???, newGraph)
    }
  }
}

final class ApplicationTier extends MockTier with ApplicationTierLike {
  type T = ApplicationTier
}

object ApplicationBehavior extends MockBehaviorOps[ApplicationTier]
object ApplicationDiscreteBehavior extends MockDiscreteBehaviorOps[ApplicationTier]
