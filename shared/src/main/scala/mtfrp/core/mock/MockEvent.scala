package mtfrp.core.mock

import hokko.{core => HC}
import mtfrp.core._
import mtfrp.core.impl.HokkoBuilder

class MockEvent[T <: MockTier: MockBuilder, A](
    private[core] val graph: ReplicationGraph
)(implicit otherBuilder: HokkoBuilder[T#Replicated])
    extends Event[T, A] {

  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def fold[B](initial: B)(f: (B, A) => B): T#IncrementalBehavior[B, A] =
    mockBuilder.incrementalBehavior(graph, f, initial)

  def unionWith(b: T#Event[A])( f3: (A, A) => A): T#Event[A] =
    mockBuilder.event(graph + b.graph)

  def collect[B](fb: A => Option[B]): T#Event[B] =
    mockBuilder.event(graph)
}

abstract class MockEventObject[SubT <: MockTier { type T = SubT }: MockBuilder]
    extends EventObject[SubT] {
  val builder = implicitly[MockBuilder[SubT]]

  def empty[A]: SubT#Event[A] = builder.event(ReplicationGraph.start)

  private[core] def apply[A](ev: HC.Event[A]): SubT#Event[A] =
    builder.event(ReplicationGraph.start)
}
