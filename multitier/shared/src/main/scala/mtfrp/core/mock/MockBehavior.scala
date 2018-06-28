package mtfrp.core.mock

import mtfrp.core._

class MockBehavior[T <: MockTier: MockBuilder, A](
    private[core] val graph: GraphState
) extends Behavior[T, A] {

  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def reverseApply[B](fb: T#Behavior[A => B]): T#Behavior[B] =
    mockBuilder.behavior(GraphState.any.combine(graph, fb.graph))

  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    mockBuilder.event(???) // TODO: just ev.bool but combine graphs)
}

abstract class MockBehaviorObject[
    SubT <: MockTier { type T = SubT }: MockBuilder]
    extends BehaviorObject[SubT] {
  private[this] val mockBuilder = implicitly[MockBuilder[SubT]]
  def constant[A](x: A): SubT#Behavior[A] =
    mockBuilder.behavior(GraphState.default)
}
