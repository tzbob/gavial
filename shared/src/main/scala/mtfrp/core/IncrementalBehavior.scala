package mtfrp
package core

import hokko.syntax.SnapshottableSyntax
import hokko.core.tc

trait IncrementalBehavior[T <: Tier, A, DeltaA] {
  private[core] def accumulator: (A, DeltaA) => A

  def changes: T#Event[A]
  def deltas: T#Event[DeltaA]
  def map[B, DeltaB](accumulator: (B, DeltaB) => B)(fa: A => B)(
      fb: DeltaA => DeltaB): T#IncrementalBehavior[B, DeltaB]

  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C]
  def toDiscreteBehavior: T#DiscreteBehavior[A]
}

object IncrementalBehavior {
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

trait IncrementalBehaviorObject[SubT <: Tier { type T = SubT }]
    extends SnapshottableSyntax {

  type IncrementalBehaviorA[A] = SubT#IncrementalBehavior[A, _]

  def constant[A](x: A): SubT#IncrementalBehavior[A, Nothing]

  implicit val mtfrpIncrementalBehaviorInstances: tc.Snapshottable[
    IncrementalBehaviorA,
    SubT#Event] =
    new tc.Snapshottable[IncrementalBehaviorA, SubT#Event] {
      override def snapshotWith[A, B, C](
          b: IncrementalBehaviorA[A],
          ev: SubT#Event[B])(f: (A, B) => C): SubT#Event[C] =
        b.snapshotWith(ev)(f)
    }
}
