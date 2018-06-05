package mtfrp.core.impl

import cats.data.Ior
import hokko.core
import mtfrp.core._

class HokkoIBehavior[T <: HokkoTier: HokkoBuilder, A, DeltaA](
    private[core] val rep: core.IBehavior[A, DeltaA],
    private[core] val initial: A,
    private[core] val graph: ReplicationGraph,
    private[core] val accumulator: (A, DeltaA) => A,
    private[core] val requiresWebSockets: Boolean
) extends IBehavior[T, A, DeltaA] {

  private[this] val builder = implicitly[HokkoBuilder[T]]

  def changes: T#Event[A] =
    builder.event(rep.changes, graph, requiresWebSockets)

  def deltas: T#Event[DeltaA] =
    builder.event(rep.deltas, graph, requiresWebSockets)

  def map[B, DeltaB](fa: A => B)(fb: DeltaA => DeltaB)(
      accumulator: (B, DeltaB) => B): T#IBehavior[B, DeltaB] =
    builder.IBehavior(
      rep.incMap(fa)(fb)(accumulator),
      fa(initial),
      graph,
      accumulator,
      requiresWebSockets
    )

  def map2[B, DeltaB, C, DeltaC](b: T#IBehavior[B, DeltaB])(
      valueFun: (A, B) => C)(
      deltaFun: (A, B, Ior[DeltaA, DeltaB]) => Option[DeltaC])(
      foldFun: (C, DeltaC) => C): T#IBehavior[C, DeltaC] =
    builder.IBehavior(
      rep.incMap2(b.rep)(valueFun)(deltaFun)(foldFun),
      valueFun(initial, b.initial),
      graph + b.graph,
      foldFun,
      requiresWebSockets || b.requiresWebSockets
    )

  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    builder.event(
      core.IBehavior.syntaxSnapshottable(rep).snapshotWith(ev.rep)(f),
      graph + ev.graph,
      ev.requiresWebSockets)

  def toDBehavior: T#DBehavior[A] =
    builder.DBehavior(rep.toDBehavior, initial, graph, requiresWebSockets)

}

abstract class HokkoIBehaviorObject[
    SubT <: HokkoTier { type T = SubT }: HokkoBuilder]
    extends IBehaviorObject[SubT] {
  private[this] val hokkoBuilder = implicitly[HokkoBuilder[SubT]]

  def constant[A, B](x: A): SubT#IBehavior[A, B] =
    hokkoBuilder.IBehavior(core.IBehavior.constant(x),
                           x,
                           ReplicationGraph.start,
                           (a: A, _: Any) => a,
                           false)
}
