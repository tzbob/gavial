package mtfrp
package core

import hokko.core

// Define all Application types
// Create all Application constructors

class ApplicationEvent[A] private[core] (graph: ReplicationGraph)
  extends MockEvent[ApplicationTier, A](graph)

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

final class ApplicationTier extends MockTier with ApplicationTierLike {
  type T = ApplicationTier
}

object ApplicationBehavior extends MockBehaviorOps[ApplicationTier]
object ApplicationDiscreteBehavior extends MockDiscreteBehaviorOps[ApplicationTier]
