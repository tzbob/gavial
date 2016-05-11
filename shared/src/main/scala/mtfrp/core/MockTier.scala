package mtfrp
package core

trait MockTier extends Tier {
  type T <: MockTier

  type Event[A] <: MockEvent[T, A]
  type Behavior[A] <: MockBehavior[T, A]
  type DiscreteBehavior[A] <: MockDiscreteBehavior[T, A]
  type IncrementalBehavior[A, DeltaA] <: MockIncBehavior[T, A, DeltaA]

  type Replicated <: HokkoTier
}

trait MockBuilder[T <: MockTier] {
  def event[A](graph: ReplicationGraph): T#Event[A]
  def behavior[A](graph: ReplicationGraph): T#Behavior[A]
  def discreteBehavior[A](graph: ReplicationGraph, init: A): T#DiscreteBehavior[A]
  def incrementalBehavior[A, DeltaA](graph: ReplicationGraph, init: A): T#IncrementalBehavior[A, DeltaA]
}
