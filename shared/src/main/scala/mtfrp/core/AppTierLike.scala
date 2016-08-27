package mtfrp
package core

private[core] trait AppTierLike extends Tier {
  type Event[A] = AppEvent[A]
  type Behavior[A] = AppBehavior[A]
  type DiscreteBehavior[A] = AppDiscreteBehavior[A]
  type IncrementalBehavior[A, DeltaA] = AppIncBehavior[A, DeltaA]

  type Replicated = ClientTier
}
