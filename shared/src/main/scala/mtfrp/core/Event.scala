package mtfrp
package core

import cats.syntax.FunctorSyntax
import hokko.{core => HC}
import hokko.core.tc

trait Event[T <: Tier, A] {
  private[core] val graph: ReplicationGraph

  def fold[B, AA >: A](initial: B)(f: (B,
                                       AA) => B): T#IncrementalBehavior[B, AA]

  def unionWith[B, C, AA >: A](b: T#Event[B])(f1: AA => C)(f2: B => C)(
      f3: (AA, B) => C): T#Event[C]

  def collect[B, AA >: A](fb: A => Option[B]): T#Event[B]
}

trait EventObject[T <: Tier]
    extends hokko.syntax.EventSyntax
    with FunctorSyntax {
  def empty[A]: T#Event[A]
  private[core] def apply[A](ev: HC.Event[A]): T#Event[A]

  def makeInstances[SubT <: T { type T = SubT }]
    : tc.Event[SubT#Event, SubT#IncrementalBehavior] =
    new tc.Event[SubT#Event, SubT#IncrementalBehavior] {
      override def fold[A, DeltaA](ev: SubT#Event[DeltaA], initial: A)(
          f: (A, DeltaA) => A): SubT#IncrementalBehavior[A, DeltaA] =
        ev.fold(initial)(f)

      override def unionWith[B, C, A](a: SubT#Event[A])(b: SubT#Event[B])(
          f1: (A) => C)(f2: (B) => C)(f3: (A, B) => C): SubT#Event[C] =
        a.unionWith(b)(f1)(f2)(f3)

      override def collect[B, A](ev: SubT#Event[A])(
          fb: (A) => Option[B]): SubT#Event[B] =
        ev.collect(fb)
    }
}
