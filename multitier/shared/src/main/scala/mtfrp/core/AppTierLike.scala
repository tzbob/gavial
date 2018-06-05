package mtfrp
package core

private[core] trait AppTierLike extends Tier {
  type T = AppTier

  type Event[A]                       = AppEvent[A]
  type Behavior[A]                    = AppBehavior[A]
  type DBehavior[A]            = AppDBehavior[A]
  type IBehavior[A, DeltaA] = AppIBehavior[A, DeltaA]

  type Replicated = ClientTier
}
