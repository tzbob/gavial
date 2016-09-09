package mtfrp.core.impl

import hokko.core
import mtfrp.core._
import mtfrp.core.mock.MockTier

trait HokkoTier extends Tier {
  type T <: HokkoTier
  type Event[A] <: HokkoEvent[T, A]
  type Behavior[A] <: HokkoBehavior[T, A]
  type DiscreteBehavior[A] <: HokkoDiscreteBehavior[T, A]
  type IncrementalBehavior[A, DeltaA] <: HokkoIncBehavior[T, A, DeltaA]

  type Replicated <: MockTier
}

trait HokkoBuilder[T <: HokkoTier] {
  def event[A](rep: core.Event[A], graph: ReplicationGraph): T#Event[A]
  def behavior[A](rep: core.CBehavior[A],
                  graph: ReplicationGraph): T#Behavior[A]
  def discreteBehavior[A](
      rep: core.DBehavior[A],
      initial: A,
      graph: ReplicationGraph
  ): T#DiscreteBehavior[A]
  def incrementalBehavior[A, DeltaA](
      rep: core.IBehavior[A, DeltaA],
      initial: A,
      graph: ReplicationGraph,
      accumulator: (A, DeltaA) => A
  ): T#IncrementalBehavior[A, DeltaA]
}
