package mtfrp
package core

import cats.Applicative
import cats.syntax.{ApplicativeSyntax, ApplySyntax, FunctorSyntax}
import hokko.core.tc
import hokko.syntax.SnapshottableSyntax

trait DBehavior[T <: Tier, A] {
  private[core] val graph: GraphState

  def changes(): T#Event[A]
  def toBehavior: T#Behavior[A]

  def reverseApply[B](fb: T#DBehavior[A => B]): T#DBehavior[B]
  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C]
}

trait DBehaviorObject[SubT <: Tier { type T = SubT }]
    extends ApplicativeSyntax
    with FunctorSyntax
    with ApplySyntax
    with SnapshottableSyntax[SubT#Event, SubT#DBehavior] {
  def constant[A](x: A): SubT#DBehavior[A]

  def delayed[A](db: => SubT#DBehavior[A], init: A): SubT#DBehavior[A]

  implicit val mtfrpDBehaviorInstances: tc.Snapshottable[
    SubT#DBehavior,
    SubT#Event] with Applicative[SubT#DBehavior] =
    new tc.Snapshottable[SubT#DBehavior, SubT#Event]
    with Applicative[SubT#DBehavior] {
      override def snapshotWith[A, B, C](
          b: SubT#DBehavior[A],
          ev: SubT#Event[B])(f: (A, B) => C): SubT#Event[C] =
        b.snapshotWith(ev)(f)

      override def pure[A](x: A): SubT#DBehavior[A] =
        DBehaviorObject.this.constant(x)

      override def ap[A, B](ff: SubT#DBehavior[(A) => B])(
          fa: SubT#DBehavior[A]): SubT#DBehavior[B] =
        fa.reverseApply(ff)
    }
}
