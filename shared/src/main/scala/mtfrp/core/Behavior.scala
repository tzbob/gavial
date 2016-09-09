package mtfrp
package core

import cats.Applicative
import cats.syntax.{ApplicativeSyntax, ApplySyntax}
import hokko.core.tc
import hokko.syntax.SnapshottableSyntax

trait Behavior[T <: Tier, A] {
  def reverseApply[B, AA >: A](fb: T#Behavior[AA => B]): T#Behavior[B]
  def snapshotWith[B, AA >: A, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C]
}

trait BehaviorObject[SubT <: Tier { type T = SubT }]
    extends ApplicativeSyntax
    with ApplySyntax
    with SnapshottableSyntax {
  def constant[A](x: A): SubT#Behavior[A]

  def makeInstances: tc.Snapshottable[SubT#Behavior, SubT#Event] with Applicative[
    SubT#Behavior] =
    new tc.Snapshottable[SubT#Behavior, SubT#Event]
    with Applicative[SubT#Behavior] {
      override def snapshotWith[A, B, C](
          b: SubT#Behavior[A],
          ev: SubT#Event[B])(f: (A, B) => C): SubT#Event[C] =
        b.snapshotWith(ev)(f)

      override def pure[A](x: A): SubT#Behavior[A] =
        BehaviorObject.this.constant(x)

      override def ap[A, B](ff: SubT#Behavior[(A) => B])(
          fa: SubT#Behavior[A]): SubT#Behavior[B] =
        fa.reverseApply(ff)
    }
}
