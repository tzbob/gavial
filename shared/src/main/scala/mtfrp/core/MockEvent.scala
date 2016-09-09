package mtfrp
package core

import hokko.{core => HC}

class MockEvent[T <: MockTier: MockBuilder, A](
    private[core] val graph: ReplicationGraph
)(implicit otherBuilder: HokkoBuilder[T#Replicated])
    extends Event[T, A] {

  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def fold[B, AA >: A](initial: B)(
      f: (B, AA) => B): T#IncrementalBehavior[B, AA] =
    mockBuilder.incrementalBehavior(graph, f, initial)

  def unionWith[B, C, AA >: A](b: T#Event[B])(f1: AA => C)(f2: B => C)(
      f3: (AA, B) => C): T#Event[C] =
    mockBuilder.event(graph + b.graph)

  def collect[B, AA >: A](fb: A => Option[B]): T#Event[B] =
    mockBuilder.event(graph)
}

abstract class MockEventObject[T <: MockTier: MockBuilder]
    extends EventObject[T] {
  val builder = implicitly[MockBuilder[T]]

  def empty[A]: T#Event[A] = builder.event(ReplicationGraph.start)

  private[core] def apply[A](ev: HC.Event[A]): T#Event[A] =
    builder.event(ReplicationGraph.start)
}
