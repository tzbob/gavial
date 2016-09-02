package mtfrp
package core

class MockDiscreteBehavior[T <: MockTier: MockBuilder, A](
    graph: ReplicationGraph,
    private[core] val initial: A
)(implicit hokkoBuilder: HokkoBuilder[T#Replicated])
    extends MockBehavior[T, A](graph)
    with DiscreteBehavior[T, A] {

  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def changes(): T#Event[A] =
    mockBuilder.event(graph)

  def discreteMap[B](f: A => B): T#DiscreteBehavior[B] =
    mockBuilder.discreteBehavior(graph, f(initial))

  def discreteMap2[B, C](b: T#DiscreteBehavior[B])(
      f: (A, B) => C): T#DiscreteBehavior[C] =
    mockBuilder.discreteBehavior(graph + b.graph, f(initial, b.initial))

  def discreteMap3[B, C, D](
      b: T#DiscreteBehavior[B],
      c: T#DiscreteBehavior[C])(f: (A, B, C) => D): T#DiscreteBehavior[D] =
    mockBuilder.discreteBehavior(graph + b.graph + c.graph,
                                 f(initial, b.initial, c.initial))

  def discreteReverseApply[B, AA >: A](
      fb: T#DiscreteBehavior[A => B]): T#DiscreteBehavior[B] =
    mockBuilder.discreteBehavior(graph + fb.graph, fb.initial(initial))
}

abstract class MockDiscreteBehaviorOps[T <: MockTier: MockBuilder]
    extends DiscreteBehaviorObject[T] {
  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def constant[A](x: A): T#DiscreteBehavior[A] =
    mockBuilder.discreteBehavior(ReplicationGraph.start, x)
}
