package mtfrp
package core

private[core] trait ServerTierLike extends Tier {
  type Event[A] = ServerEvent[A]
  type Behavior[A] = ServerBehavior[A]
  type DiscreteBehavior[A] = ServerDiscreteBehavior[A]
  type IncrementalBehavior[A, DeltaA] = ServerIncBehavior[A, DeltaA]

  type Replicated = ClientTier
}
