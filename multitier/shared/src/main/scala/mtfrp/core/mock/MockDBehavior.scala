package mtfrp.core.mock

import mtfrp.core._

class MockDBehavior[T <: MockTier: MockBuilder, A](
    private[core] val graph: GraphState,
    private[core] val initial: A
) extends DBehavior[T, A] {

  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def changes(): T#Event[A] =
    mockBuilder.event(graph)

  def reverseApply[B](fb: T#DBehavior[A => B]): T#DBehavior[B] =
    mockBuilder.DBehavior(GraphState.any.combine(graph, fb.graph),
                          fb.initial(initial))

  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    mockBuilder.event(ev.graph.mergeGraphAndEffect(this.graph))

  def toBehavior: T#Behavior[A] =
    mockBuilder.behavior(graph)
}

abstract class MockDBehaviorObject[
    SubT <: MockTier { type T = SubT }: MockBuilder]
    extends DBehaviorObject[SubT] {
  private[this] val mockBuilder = implicitly[MockBuilder[SubT]]

  def constant[A](x: A): SubT#DBehavior[A] =
    mockBuilder.DBehavior(GraphState.default, x)
}
