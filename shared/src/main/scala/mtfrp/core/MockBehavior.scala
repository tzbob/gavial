package mtfrp
package core

class MockBehavior[T <: MockTier: MockBuilder, A](
    private[core] val graph: ReplicationGraph
)(implicit hokkoBuilder: HokkoBuilder[T#Replicated])
    extends Behavior[T, A] {

  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def reverseApply[B, AA >: A](fb: T#Behavior[AA => B]): T#Behavior[B] =
    mockBuilder.behavior(graph + fb.graph)

  def snapshotWith[B, AA >: A, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    mockBuilder.event(graph + ev.graph)
}

abstract class MockBehaviorObject[ SubT <: MockTier { type T = SubT }: MockBuilder]
    extends BehaviorObject[SubT] {
  private[this] val mockBuilder = implicitly[MockBuilder[SubT]]
  def constant[A](x: A): SubT#Behavior[A] =
    mockBuilder.behavior(ReplicationGraph.start)
}
