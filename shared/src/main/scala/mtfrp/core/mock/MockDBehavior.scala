package mtfrp.core.mock

import mtfrp.core._
import mtfrp.core.impl.HokkoBuilder

class MockDBehavior[T <: MockTier: MockBuilder, A](
    private[core] val graph: ReplicationGraph,
    private[core] val initial: A
)(implicit hokkoBuilder: HokkoBuilder[T#Replicated])
    extends DBehavior[T, A] {

  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def changes(): T#Event[A] =
    mockBuilder.event(graph)

  def reverseApply[B](fb: T#DBehavior[A => B]): T#DBehavior[B] =
    mockBuilder.DBehavior(graph + fb.graph, fb.initial(initial))

  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    mockBuilder.event(graph + ev.graph)

  def toBehavior: T#Behavior[A] =
    mockBuilder.behavior(graph)
}

abstract class MockDBehaviorObject[
    SubT <: MockTier { type T = SubT }: MockBuilder]
    extends DBehaviorObject[SubT] {
  private[this] val mockBuilder = implicitly[MockBuilder[SubT]]

  def constant[A](x: A): SubT#DBehavior[A] =
    mockBuilder.DBehavior(ReplicationGraph.start, x)
}
