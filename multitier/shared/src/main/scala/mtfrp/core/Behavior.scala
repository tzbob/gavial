package mtfrp
package core

import cats.Applicative
import cats.syntax.{ApplicativeSyntax, ApplySyntax, FunctorSyntax}
import hokko.core.{Thunk, tc}
import hokko.syntax.SnapshottableSyntax

trait Behavior[T <: Tier, A] {
  private[core] val graph: GraphState

  private[core] val initial: Thunk[A]

  def reverseApply[B](fb: T#Behavior[A => B]): T#Behavior[B]
  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C]
  def snapshotWith[B, C](ev: T#DBehavior[B])(f: (A, B) => C): T#DBehavior[C]
}

trait BehaviorObject[SubT <: Tier { type T = SubT }]
    extends ApplicativeSyntax
    with FunctorSyntax
    with ApplySyntax
    with SnapshottableSyntax[SubT#Behavior] {
  def constant[A](x: A): SubT#Behavior[A]

  implicit val mtfrpDBehaviorSnapshotCBehavior
    : tc.Snapshottable[SubT#Behavior, SubT#DBehavior] =
    new tc.Snapshottable[SubT#Behavior, SubT#DBehavior] {
      override def snapshotWith[A, B, C](
          b: SubT#Behavior[A],
          db: SubT#DBehavior[B])(f: (A, B) => C): SubT#DBehavior[C] =
        b.snapshotWith(db)(f)
    }

  implicit val mtfrpBehaviorInstances
    : tc.Snapshottable[SubT#Behavior, SubT#Event]
      with Applicative[SubT#Behavior] =
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
