package mtfrp
package core

import cats.data.Ior
import hokko.syntax.SnapshottableSyntax
import hokko.core.tc

trait IBehavior[T <: Tier, A, DeltaA] {
  private[core] def accumulator: (A, DeltaA) => A

  def changes: T#Event[A]
  def deltas: T#Event[DeltaA]
  def map[B, DeltaB](fa: A => B)(fb: DeltaA => DeltaB)(
      accumulator: (B, DeltaB) => B): T#IBehavior[B, DeltaB]
  def map2[B, DeltaB, C, DeltaC](b: T#IBehavior[B, DeltaB])(
      valueFun: (A, B) => C)(
      deltaFun: (A, B, Ior[DeltaA, DeltaB]) => Option[DeltaC])(
      foldFun: (C, DeltaC) => C
  ): T#IBehavior[C, DeltaC]

  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C]
  def toDBehavior: T#DBehavior[A]
}

object IBehavior {
  private[core] def transformToNormal[X, Y](
      f: (Client => X, Client => Option[Y]) => (Client => X)
  ): (X, Y) => X = { (x, y) =>
    val xf      = (_: Client) => x
    val yf      = (_: Client) => (Some(y): Option[Y])
    val resultF = f(xf, yf)
    resultF(null) // this is fine | we want this to blow up if it gets used
  }

  private[core] def transformFromNormal[A, DeltaA](f: (A, DeltaA) => A)
    : (Map[Client, A], (Client, DeltaA)) => Map[Client, A] = {
    (acc: Map[Client, A], delta: (Client, DeltaA)) =>
      delta match {
        case (client, clientDelta) =>
          // FIXME: relying on Map.default is dangerous
          val clientAcc = acc.getOrElse(client, acc.default(client))
          acc.updated(client, f(clientAcc, clientDelta))
      }
  }
}

trait IBehaviorObject[SubT <: Tier { type T = SubT }]
    extends SnapshottableSyntax[SubT#Event, SubT#DBehavior] {

  type IBehaviorA[A] = SubT#IBehavior[A, _]

  def constant[A, B](x: A): SubT#IBehavior[A, B]

  implicit val mtfrpIBehaviorInstances
    : tc.Snapshottable[IBehaviorA, SubT#Event] =
    new tc.Snapshottable[IBehaviorA, SubT#Event] {
      override def snapshotWith[A, B, C](
          b: IBehaviorA[A],
          ev: SubT#Event[B])(f: (A, B) => C): SubT#Event[C] =
        b.snapshotWith(ev)(f)
    }
}
