package mtfrp
package core

import hokko.{core => HC}
import io.circe._

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

  // Derived ops
  def dropIf[B](f: A => Boolean): T#Event[A] =
    mockBuilder.event(graph)
  def hold[AA >: A](initial: AA): T#DiscreteBehavior[AA] =
    mockBuilder.discreteBehavior(graph, initial)

  def map[B](f: A => B): T#Event[B] =
    mockBuilder.event(graph)
  def mergeWith[AA >: A](events: T#Event[AA]*): T#Event[Seq[AA]] =
    mockBuilder.event(ReplicationGraph.combine(graph +: events.map(_.graph)))
  def unionLeft[AA >: A](other: T#Event[AA]): T#Event[AA] =
    mockBuilder.event(graph + other.graph)
  def unionRight[AA >: A](other: T#Event[AA]): T#Event[AA] =
    mockBuilder.event(graph + other.graph)
}
