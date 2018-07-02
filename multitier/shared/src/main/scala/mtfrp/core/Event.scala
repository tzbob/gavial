package mtfrp
package core

import cats.syntax.FunctorSyntax
import hokko.{core => HC}
import hokko.core.tc

trait Event[T <: Tier, A] {
  private[core] val graph: GraphState

  def fold[B](initial: B)(f: (B, A) => B): T#IBehavior[B, A]

  def unionWith(b: T#Event[A])(f: (A, A) => A): T#Event[A]

  def collect[B](fb: A => Option[B]): T#Event[B]
}

trait EventObject[SubT <: Tier { type T = SubT }]
    extends hokko.syntax.EventSyntax[SubT#Event, SubT#IBehavior]
    with FunctorSyntax {
  def empty[A]: SubT#Event[A]
  private[core] def apply[A](ev: HC.Event[A],
                             graphState: GraphState): SubT#Event[A]

  implicit val mtfrpEventInstances: tc.Event[SubT#Event, SubT#IBehavior] =
    new tc.Event[SubT#Event, SubT#IBehavior] {
      override def fold[A, DeltaA](ev: SubT#Event[DeltaA], initial: A)(
          f: (A, DeltaA) => A): SubT#IBehavior[A, DeltaA] =
        ev.fold(initial)(f)

      override def unionWith[A](a: SubT#Event[A])(b: SubT#Event[A])(
          f: (A, A) => A): SubT#Event[A] = a.unionWith(b)(f)

      override def collect[B, A](ev: SubT#Event[A])(
          fb: (A) => Option[B]): SubT#Event[B] =
        ev.collect(fb)
    }
}
