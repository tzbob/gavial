package mtfrp
package core
import mtfrp.core.Tier.Concrete

private[core] trait ClientTierLike extends Tier {
  implicit val companion: Concrete[ClientTier] = new Concrete[ClientTier] {
    lazy val tier = ClientTier
  }
  type T = ClientTier

  type Event[A]             = ClientEvent[A]
  type Behavior[A]          = ClientBehavior[A]
  type DBehavior[A]         = ClientDBehavior[A]
  type IBehavior[A, DeltaA] = ClientIBehavior[A, DeltaA]

  val Event     = ClientEvent
  val Behavior  = ClientBehavior
  val DBehavior = ClientDBehavior
  val IBehavior = ClientIBehavior

  val Async = ClientAsync

  type Replicated = AppTier
}
