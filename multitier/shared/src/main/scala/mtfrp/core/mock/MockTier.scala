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
  def event[A](graph: GraphState): T#Event[A]
  def behavior[A](graph: GraphState): T#Behavior[A]
  def DBehavior[A](graph: => GraphState, initial: A): T#DBehavior[A]
  def IBehavior[A, DeltaA](graph: GraphState,
                           accumulator: (A, DeltaA) => A,
                           initial: A): T#IBehavior[A, DeltaA]
}
