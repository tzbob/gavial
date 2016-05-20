package mtfrp
package core

import hokko.core

// Define all Client types
// Create all Client constructors

class ClientEvent[A] private[core] (graph: ReplicationGraph)
    extends MockEvent[ClientTier, A](graph)

class ClientBehavior[A] private[core] (graph: ReplicationGraph)
    extends MockBehavior[ClientTier, A](graph)

class ClientDiscreteBehavior[A] private[core] (
  graph: ReplicationGraph,
  init: A
) extends MockDiscreteBehavior[ClientTier, A](init, graph)

class ClientIncBehavior[A, DeltaA] private[core] (
  graph: ReplicationGraph,
  init: A
) extends MockIncBehavior[ClientTier, A, DeltaA](init, graph)

final class ClientTier extends MockTier with ClientTierLike {
  type T = ClientTier
}

object ClientBehavior extends MockBehaviorOps[ClientTier]
object ClientDiscreteBehavior extends MockDiscreteBehaviorOps[ClientTier]
