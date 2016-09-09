package mtfrp
package core

trait SessionTier extends Tier {
  type T = SessionTier

  type Event[A] = SessionEvent[A]
  // type Behavior[A]
  // type DiscreteBehavior[A]
  // type IncrementalBehavior[A, DeltaA]

  type Replicated = ClientTier
}
