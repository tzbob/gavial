package mtfrp
package core

private[core] trait ClientTierLike extends Tier {
  type T = ClientTier

  type Event[A]                       = ClientEvent[A]
  type Behavior[A]                    = ClientBehavior[A]
  type DBehavior[A]            = ClientDBehavior[A]
  type IBehavior[A, DeltaA] = ClientIBehavior[A, DeltaA]

  type Replicated = AppTier
}
