package mtfrp
package core

import cats.Applicative
import cats.syntax.{ApplicativeSyntax, ApplySyntax, FunctorSyntax}
import hokko.core.tc
import hokko.syntax.SnapshottableSyntax

trait DiscreteBehavior[T <: Tier, A] {
  def changes(): T#Event[A]
  def toBehavior: T#Behavior[A]

  def reverseApply[B](fb: T#DiscreteBehavior[A => B]): T#DiscreteBehavior[B]
  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C]
}

trait DiscreteBehaviorObject[SubT <: Tier { type T = SubT }]
    extends ApplicativeSyntax
    with FunctorSyntax
    with ApplySyntax
    with SnapshottableSyntax[SubT#Event, SubT#DiscreteBehavior] {
  def constant[A](x: A): SubT#DiscreteBehavior[A]

  implicit val mtfrpDBehaviorInstances: tc.Snapshottable[
    SubT#DiscreteBehavior,
    SubT#Event] with Applicative[SubT#DiscreteBehavior] =
    new tc.Snapshottable[SubT#DiscreteBehavior, SubT#Event]
    with Applicative[SubT#DiscreteBehavior] {
      override def snapshotWith[A, B, C](
          b: SubT#DiscreteBehavior[A],
          ev: SubT#Event[B])(f: (A, B) => C): SubT#Event[C] =
        b.snapshotWith(ev)(f)

      override def pure[A](x: A): SubT#DiscreteBehavior[A] =
        DiscreteBehaviorObject.this.constant(x)

      override def ap[A, B](ff: SubT#DiscreteBehavior[(A) => B])(
          fa: SubT#DiscreteBehavior[A]): SubT#DiscreteBehavior[B] =
        fa.reverseApply(ff)
    }
}
