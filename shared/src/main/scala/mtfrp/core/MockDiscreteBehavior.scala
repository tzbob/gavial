package mtfrp
package core

class MockDiscreteBehavior[T <: MockTier: MockBuilder, A](
    private[core] val graph: ReplicationGraph,
    private[core] val initial: A
)(implicit hokkoBuilder: HokkoBuilder[T#Replicated])
    extends DiscreteBehavior[T, A] {

  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def changes(): T#Event[A] =
    mockBuilder.event(graph)

  def reverseApply[B, AA >: A](
      fb: T#DiscreteBehavior[AA => B]): T#DiscreteBehavior[B] =
    mockBuilder.discreteBehavior(graph + fb.graph, fb.initial(initial))

  def snapshotWith[B, AA >: A, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    mockBuilder.event(graph + ev.graph)

  def toBehavior: T#Behavior[A] =
    mockBuilder.behavior(graph)
}

abstract class MockDiscreteBehaviorObject[
    SubT <: MockTier { type T = SubT }: MockBuilder]
    extends DiscreteBehaviorObject[SubT] {
  private[this] val mockBuilder = implicitly[MockBuilder[SubT]]

  def constant[A](x: A): SubT#DiscreteBehavior[A] =
    mockBuilder.discreteBehavior(ReplicationGraph.start, x)
}
