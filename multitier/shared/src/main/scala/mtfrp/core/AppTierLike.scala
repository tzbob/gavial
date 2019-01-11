package mtfrp
package core

import mtfrp.core.Tier.Concrete

private[core] trait AppTierLike extends Tier {
  implicit val companion: Concrete[AppTier] = new Concrete[AppTier] {
    lazy val tier = AppTier
  }

  type T = AppTier

  type Event[A]             = AppEvent[A]
  type Behavior[A]          = AppBehavior[A]
  type DBehavior[A]         = AppDBehavior[A]
  type IBehavior[A, DeltaA] = AppIBehavior[A, DeltaA]

  val Event     = AppEvent
  val Behavior  = AppBehavior
  val DBehavior = AppDBehavior
  val IBehavior = AppIBehavior

  val Async = AppAsync

  type Replicated = ClientTier
}
