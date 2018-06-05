package mtfrp.core.mock

import mtfrp.core._

class MockBehavior[T <: MockTier: MockBuilder, A](
    private[core] val graph: ReplicationGraph,
    private[core] val requiresWebSockets: Boolean
) extends Behavior[T, A] {

  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def reverseApply[B](fb: T#Behavior[A => B]): T#Behavior[B] =
    mockBuilder.behavior(graph + fb.graph,
                         requiresWebSockets || fb.requiresWebSockets)

  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    mockBuilder.event(graph + ev.graph, ev.requiresWebSockets)
}

abstract class MockBehaviorObject[
    SubT <: MockTier { type T = SubT }: MockBuilder]
    extends BehaviorObject[SubT] {
  private[this] val mockBuilder = implicitly[MockBuilder[SubT]]
  def constant[A](x: A): SubT#Behavior[A] =
    mockBuilder.behavior(ReplicationGraph.start, false)
}
