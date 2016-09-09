package mtfrp
package core

private[core] trait ClientTierLike extends Tier {
  type T = ClientTier

  type Event[A]                       = ClientEvent[A]
  type Behavior[A]                    = ClientBehavior[A]
  type DiscreteBehavior[A]            = ClientDiscreteBehavior[A]
  type IncrementalBehavior[A, DeltaA] = ClientIncBehavior[A, DeltaA]

  type Replicated = AppTier
}
