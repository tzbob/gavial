package mtfrp.core.mock

import hokko.core.Thunk
import mtfrp.core._

class MockBehavior[T <: MockTier: MockBuilder, A](
    private[core] val graph: GraphState,
    private[core] val initial: Thunk[A]
) extends Behavior[T, A] {

  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def reverseApply[B](fb: T#Behavior[A => B]): T#Behavior[B] =
    mockBuilder.behavior(GraphState.any.combine(graph, fb.graph),
                         fb.initial.flatMap(f => this.initial.map(f(_))))

  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    mockBuilder.event(ev.graph.mergeGraphAndEffect(this.graph))

  def snapshotWith[B, C](ev: T#DBehavior[B])(f: (A, B) => C): T#DBehavior[C] =
    mockBuilder.DBehavior(ev.graph.mergeGraphAndEffect(this.graph),
                          this.initial.map(a => f(a, ev.initial)).force)
}

abstract class MockBehaviorObject[
    SubT <: MockTier { type T = SubT }: MockBuilder]
    extends BehaviorObject[SubT] {
  private[this] val mockBuilder = implicitly[MockBuilder[SubT]]
  def constant[A](x: A): SubT#Behavior[A] =
    mockBuilder.behavior(GraphState.default, Thunk.eager(x))
}
