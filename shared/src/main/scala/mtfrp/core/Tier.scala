package mtfrp
package core

trait Tier {
  type Event[A]
  type Behavior[A]
  type DiscreteBehavior[A]
  type IncrementalBehavior[A, DeltaA]

  type Replicated <: Tier
}
