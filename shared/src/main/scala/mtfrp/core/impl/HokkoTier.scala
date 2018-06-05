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
  def event[A](rep: core.Event[A],
               graph: ReplicationGraph,
               requiresWebSockets: Boolean): T#Event[A]
  def behavior[A](rep: core.CBehavior[A],
                  graph: ReplicationGraph,
                  requiresWebSockets: Boolean): T#Behavior[A]
  def DBehavior[A](
      rep: core.DBehavior[A],
      initial: A,
      graph: ReplicationGraph,
      requiresWebSockets: Boolean
  ): T#DBehavior[A]
  def IBehavior[A, DeltaA](
      rep: core.IBehavior[A, DeltaA],
      initial: A,
      graph: ReplicationGraph,
      accumulator: (A, DeltaA) => A,
      requiresWebSockets: Boolean
  ): T#IBehavior[A, DeltaA]
}
