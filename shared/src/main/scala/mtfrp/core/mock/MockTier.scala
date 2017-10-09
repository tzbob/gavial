package mtfrp.core.mock

import mtfrp.core._
import mtfrp.core.impl.HokkoTier

trait MockTier extends Tier {
  type T <: MockTier

  type Event[A] <: MockEvent[T, A]
  type Behavior[A] <: MockBehavior[T, A]
  type DBehavior[A] <: MockDBehavior[T, A]
  type IBehavior[A, DeltaA] <: MockIBehavior[T, A, DeltaA]

  type Replicated <: HokkoTier
}

trait MockBuilder[T <: MockTier] {
  def event[A](graph: ReplicationGraph): T#Event[A]
  def behavior[A](graph: ReplicationGraph): T#Behavior[A]
  def DBehavior[A](graph: ReplicationGraph,
                          initial: A): T#DBehavior[A]
  def IBehavior[A, DeltaA](
      graph: ReplicationGraph,
      accumulator: (A, DeltaA) => A,
      initial: A
  ): T#IBehavior[A, DeltaA]
}
