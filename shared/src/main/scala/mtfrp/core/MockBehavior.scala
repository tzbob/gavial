package mtfrp
package core

class MockBehavior[T <: MockTier: MockBuilder, A](
    private[core] val graph: ReplicationGraph
)(implicit hokkoBuilder: HokkoBuilder[T#Replicated])
    extends Behavior[T, A] {

  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def map[B](f: A => B): T#Behavior[B] =
    mockBuilder.behavior(graph)

  def map2[B, C](b: T#Behavior[B])(f: (A, B) => C): T#Behavior[C] =
    mockBuilder.behavior(graph + b.graph)

  def map3[B, C, D](b: T#Behavior[B], c: T#Behavior[C])(
      f: (A, B, C) => D): T#Behavior[D] =
    mockBuilder.behavior(graph + b.graph + c.graph)

  def reverseApply[B, AA >: A](fb: T#Behavior[AA => B]): T#Behavior[B] =
    mockBuilder.behavior(graph + fb.graph)

  def sampledBy(ev: T#Event[_]): T#Event[A] =
    mockBuilder.event(graph + ev.graph)

  def snapshotWith[B, AA >: A](ev: T#Event[AA => B]): T#Event[B] =
    mockBuilder.event(graph + ev.graph)

}

abstract class MockBehaviorOps[T <: MockTier: MockBuilder]
    extends BehaviorObject[T] {
  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def constant[A](x: A): T#Behavior[A] =
    mockBuilder.behavior(ReplicationGraph.start)
}
