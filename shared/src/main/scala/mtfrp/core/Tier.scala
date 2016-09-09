package mtfrp
package core

trait Tier {
  type T <: Tier
  type Event[A] <: core.Event[T, A]
  type Behavior[A] <: core.Behavior[T, A]
  type DiscreteBehavior[A] <: core.DiscreteBehavior[T, A]
  type IncrementalBehavior[A, DeltaA] <: core.IncrementalBehavior[T, A, DeltaA]

  type Replicated <: Tier
}
