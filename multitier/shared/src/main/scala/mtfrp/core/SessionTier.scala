package mtfrp.core
import mtfrp.core.Tier.Concrete

trait SessionTier extends Tier {
  implicit val companion: Concrete[SessionTier] = new Concrete[SessionTier] {
    lazy val tier = SessionTier
  }
  type T = SessionTier

  type Event[A]             = SessionEvent[A]
  type Behavior[A]          = SessionBehavior[A]
  type DBehavior[A]         = SessionDBehavior[A]
  type IBehavior[A, DeltaA] = SessionIBehavior[A, DeltaA]

  val Event     = SessionEvent
  val Behavior  = SessionBehavior
  val DBehavior = SessionDBehavior
  val IBehavior = SessionIBehavior

  val Async = SessionAsync

  type Replicated = ClientTier
}

final object SessionTier extends SessionTier
