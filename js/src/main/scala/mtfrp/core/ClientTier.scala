package mtfrp
package core

import hokko.core

// Define all Client types
// Create all Client constructors

class ClientEvent[A] private[core] (
  rep: core.Event[A],
  graph: ReplicationGraph
) extends HokkoEvent[ClientTier, A](rep, graph)

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
  graph: ReplicationGraph
) extends HokkoIncBehavior[ClientTier, A, DeltaA](rep, initial, graph)

final class ClientTier extends HokkoTier with ClientTierLike {
  type T = ClientTier
}

object ClientBehavior extends HokkoBehaviorOps[ClientTier]
object ClientDiscreteBehavior extends HokkoDiscreteBehaviorOps[ClientTier]
