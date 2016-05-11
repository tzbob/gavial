package mtfrp
package core

import hokko.core

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
  def behavior[A](rep: core.Behavior[A], graph: ReplicationGraph): T#Behavior[A]
  def discreteBehavior[A](
    rep: core.DiscreteBehavior[A],
    initial: A,
    graph: ReplicationGraph
  ): T#DiscreteBehavior[A]
  def incrementalBehavior[A, DeltaA](
    rep: core.IncrementalBehavior[A, DeltaA],
    initial: A,
    graph: ReplicationGraph
  ): T#IncrementalBehavior[A, DeltaA]
}
