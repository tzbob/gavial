package mtfrp.core

trait SessionTier extends Tier {
  type T = SessionTier

  type Event[A]                       = SessionEvent[A]
  type Behavior[A]                    = SessionBehavior[A]
  type DiscreteBehavior[A]            = SessionDiscreteBehavior[A]
  type IncrementalBehavior[A, DeltaA] = SessionIncBehavior[A, DeltaA]

  type Replicated = ClientTier
}
