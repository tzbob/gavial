package mtfrp
package core

import cats.data.Ior
import hokko.syntax.{SnapshottableOps, SnapshottableSyntax}
import hokko.core.tc
import hokko.core.tc.Snapshottable

trait IBehavior[T <: Tier, A, DeltaA] {
  private[core] val graph: GraphState

  private[core] val initial: A
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

  /**
    *
    * Transform a Map Client function to a normal function, a fresh client is
    * generated to perform the operation. THIS DISCARDS ALL CLIENT SPECIFIC
    * OPERATIONS IN THE ORIGINAL FUNCTION F.
    * @param f
    * @tparam X
    * @tparam Y
    * @return
    */
  private[core] def transformFromMap[X, Y](
      f: (Map[Client, X], Map[Client, Y]) => Map[Client, X]): (X, Y) => X = {
    (x, y) =>
      val c       = ClientGenerator.fresh
      val xMap    = Map(c -> x)
      val yMap    = Map(c -> y)
      val resultF = f(xMap, yMap)
      resultF(c)
  }

  private[core] def transformToNormal[X, Y](
      f: (Client => X, Client => Option[Y]) => (Client => X)
  ): (X, Y) => X = { (x, y) =>
    val xf      = (_: Client) => x
    val yf      = (_: Client) => (Some(y): Option[Y])
    val resultF = f(xf, yf)
    resultF(null) // this is fine | we want this to blow up if it gets used
  }

  private[core] def transformFromNormal[A, DeltaA](initial: A,
                                                   f: (A, DeltaA) => A)
    : (Map[Client, A], (Client, DeltaA)) => Map[Client, A] = {
    (acc: Map[Client, A], delta: (Client, DeltaA)) =>
      delta match {
        case (client, clientDelta) =>
          val clientAcc = acc.getOrElse(client, initial)
          acc.updated(client, f(clientAcc, clientDelta))
      }
  }

  private[core] def transformFromNormalToSetClientChangeMap[A, DeltaA](
      initial: A,
      f: (A, DeltaA) => A) = {
    (acc: Map[Client, A], change: (Map[Client, DeltaA], Set[ClientChange])) =>
      val transformed         = transformFromNormal(initial, f)
      val (deltaMap, changes) = change

      val connects    = changes.collect { case Connected(c)    => c }
      val disconnects = changes.collect { case Disconnected(c) => c }

      val connectsWithoutPulse = connects -- deltaMap.keys
      val accWithConnects      = acc ++ connectsWithoutPulse.map(_ -> initial)

      val changesWithoutDisconnects = deltaMap -- disconnects

      changesWithoutDisconnects.foldLeft(accWithConnects)(transformed)
  }

  private[core] def transformFromNormalToClientChangeMap[A, DeltaA](
      initial: A,
      f: (A, DeltaA) => A) = {
    (acc: Map[Client, A],
     change: (Map[Client, DeltaA], Option[ClientChange])) =>
      val (deltaMap, changeOpt) = change
      transformFromNormalToSetClientChangeMap(initial, f)(
        acc,
        deltaMap -> changeOpt.toSet)
  }

  private[core] def transformFromNormalToClientChange[A, DeltaA](
      initial: A,
      f: (A, DeltaA) => A) = {
    (acc: Map[Client, A], ior: Ior[(Client, DeltaA), ClientChange]) =>
      val transformed = transformFromNormal(initial, f)
      ior match {
        case Ior.Both(_, Disconnected(c)) => acc - c
        case Ior.Both(pulse, _)           => transformed(acc, pulse)
        case Ior.Left(pulse)              => transformed(acc, pulse)
        case Ior.Right(Connected(c))      => acc + (c -> initial)
        case Ior.Right(Disconnected(c))   => acc - c
        case _ =>
          throw new RuntimeException("Impossible accumulation crash")
      }
  }
}

trait IBehaviorObject[SubT <: Tier { type T = SubT }] {
  implicit def syntaxSnapshottable[A, DA](b: SubT#IBehavior[A, DA])(
      implicit ev: Snapshottable[SubT#IBehavior[?, DA], SubT#Event])
    : SnapshottableOps[SubT#IBehavior[?, DA], A] =
    new SnapshottableOps[SubT#IBehavior[?, DA], A](b)

  def constant[A, B](x: A): SubT#IBehavior[A, B]

  implicit def mtfrpIBehaviorInstances[DA]
    : tc.Snapshottable[SubT#IBehavior[?, DA], SubT#Event] =
    new tc.Snapshottable[SubT#IBehavior[?, DA], SubT#Event] {
      override def snapshotWith[A, B, C](
          b: SubT#IBehavior[A, DA],
          ev: SubT#Event[B])(f: (A, B) => C): SubT#Event[C] =
        b.snapshotWith(ev)(f)
    }
}
