package mtfrp
package core

trait Tier {
  type T <: Tier
  type Event[A] <: core.Event[T, A]
  type Behavior[A] <: core.Behavior[T, A]
  type DBehavior[A] <: core.DBehavior[T, A]
  type IBehavior[A, DeltaA] <: core.IBehavior[T, A, DeltaA]

  type Replicated <: Tier
}
