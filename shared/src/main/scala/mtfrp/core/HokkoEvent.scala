package mtfrp
package core

import hokko.core
import io.circe._

private[core] class HokkoEvent[T <: HokkoTier: HokkoBuilder, A](
  private[core] val rep: core.Event[A],
  private[core] val graph: ReplicationGraph
) extends Event[T, A] {

  type Tier = T

  private[this] val hokkoBuilder = implicitly[HokkoBuilder[T]]

  def fold[B, AA >: A](initial: B)(f: (B, AA) => B): T#IncrementalBehavior[B, AA] =
    hokkoBuilder.incrementalBehavior(rep.fold(initial)(f), initial, graph)

  def unionWith[B, C, AA >: A](b: T#Event[B])(f1: AA => C)(f2: B => C)(f3: (AA, B) => C): T#Event[C] =
    hokkoBuilder.event(rep.unionWith(b.rep)(f1)(f2)(f3), graph + b.graph)

  def collect[B, AA >: A](fb: A => Option[B]): T#Event[B] =
    hokkoBuilder.event(rep.collect(fb), graph)

  // Derived ops
  def dropIf[B](f: A => Boolean): T#Event[A] =
    hokkoBuilder.event(rep.dropIf(f), graph)

  def hold[AA >: A](initial: AA): T#DiscreteBehavior[AA] =
    hokkoBuilder.discreteBehavior(rep.hold(initial), initial, graph)

  def map[B](f: A => B): T#Event[B] =
    hokkoBuilder.event(rep.map(f), graph)

  def mergeWith[AA >: A](events: T#Event[AA]*): T#Event[Seq[AA]] =
    hokkoBuilder.event(
      rep.mergeWith(events.map(_.rep): _*),
      ReplicationGraph.combine(graph +: events.map(_.graph))
    )

  def unionLeft[AA >: A](other: T#Event[AA]): T#Event[AA] =
    hokkoBuilder.event(rep.unionLeft(other.rep), graph + other.graph)

  def unionRight[AA >: A](other: T#Event[AA]): T#Event[AA] =
    hokkoBuilder.event(rep.unionRight(other.rep), graph + other.graph)
}
