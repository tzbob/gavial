package mtfrp
package core

private[core] trait ApplicationTierLike extends Tier {
  type Event[A] = ApplicationEvent[A]
  type Behavior[A] = ApplicationBehavior[A]
  type DiscreteBehavior[A] = ApplicationDiscreteBehavior[A]
  type IncrementalBehavior[A, DeltaA] = ApplicationIncBehavior[A, DeltaA]

  type Replicated = ClientTier
}
