package mtfrp.core.impl

import hokko.core
import mtfrp.core._
import mtfrp.core.mock.MockTier

trait HokkoTier extends Tier {
  type T <: HokkoTier
  type Event[A] <: HokkoEvent[T, A]
  type Behavior[A] <: HokkoBehavior[T, A]
  type DBehavior[A] <: HokkoDBehavior[T, A]
  type IBehavior[A, DeltaA] <: HokkoIBehavior[T, A, DeltaA]

  type Replicated <: MockTier
}

trait HokkoBuilder[T <: HokkoTier] {
  def event[A](rep: core.Event[A], graph: GraphState): T#Event[A]
  def behavior[A](rep: core.CBehavior[A], graph: GraphState): T#Behavior[A]
  def DBehavior[A](rep: core.DBehavior[A], graph: => GraphState): T#DBehavior[A]
  def IBehavior[A, DeltaA](rep: core.IBehavior[A, DeltaA],
                           graph: GraphState): T#IBehavior[A, DeltaA]
}
