package mtfrp
package core

import hokko.core

// Define all Application types
// Create all Application constructors

class ApplicationEvent[A] private[core] (
  rep: core.Event[A],
  graph: ReplicationGraph
) extends HokkoEvent[ApplicationTier, A](rep, graph)
  with HokkoToClientReplicable[A]

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

final class ApplicationTier extends HokkoTier with ApplicationTierLike {
  type T = ApplicationTier
}

object ApplicationBehavior extends HokkoBehaviorOps[ApplicationTier]
object ApplicationDiscreteBehavior extends HokkoDiscreteBehaviorOps[ApplicationTier]
