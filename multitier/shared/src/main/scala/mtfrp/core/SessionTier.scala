package mtfrp.core

trait SessionTier extends Tier {
  type T = SessionTier

  type Event[A]                       = SessionEvent[A]
  type Behavior[A]                    = SessionBehavior[A]
  type DBehavior[A]            = SessionDBehavior[A]
  type IBehavior[A, DeltaA] = SessionIBehavior[A, DeltaA]

  type Replicated = ClientTier
}
