package mtfrp.core.mock

import mtfrp.core._

class MockDBehavior[T <: MockTier: MockBuilder, A](
    private[core] val graph: ReplicationGraph,
    private[core] val initial: A,
    private[core] val requiresWebSockets: Boolean
) extends DBehavior[T, A] {

  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def changes(): T#Event[A] =
    mockBuilder.event(graph, requiresWebSockets)

  def reverseApply[B](fb: T#DBehavior[A => B]): T#DBehavior[B] =
    mockBuilder.DBehavior(graph + fb.graph,
                          fb.initial(initial),
                          requiresWebSockets || fb.requiresWebSockets)

  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    mockBuilder.event(graph + ev.graph, ev.requiresWebSockets)

  def toBehavior: T#Behavior[A] =
    mockBuilder.behavior(graph, requiresWebSockets)
}

abstract class MockDBehaviorObject[
    SubT <: MockTier { type T = SubT }: MockBuilder]
    extends DBehaviorObject[SubT] {
  private[this] val mockBuilder = implicitly[MockBuilder[SubT]]

  def constant[A](x: A): SubT#DBehavior[A] =
    mockBuilder.DBehavior(ReplicationGraph.start, x, false)
}
