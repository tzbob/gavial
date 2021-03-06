package mtfrp.core.impl

import cats.data.Ior
import hokko.core
import mtfrp.core._

class HokkoIBehavior[T <: HokkoTier: HokkoBuilder, A, DeltaA](
    private[core] val rep: core.IBehavior[A, DeltaA],
    private[core] val graph: GraphState
) extends IBehavior[T, A, DeltaA] {

  private[core] lazy val initial     = rep.initial
  private[core] lazy val accumulator = rep.accumulator

  private[this] val builder = implicitly[HokkoBuilder[T]]

  def changes: T#Event[A] =
    builder.event(rep.changes, graph)

  def deltas: T#Event[DeltaA] =
    builder.event(rep.deltas, graph)

  def map[B, DeltaB](fa: A => B)(fb: DeltaA => DeltaB)(
      accumulator: (B, DeltaB) => B): T#IBehavior[B, DeltaB] =
    builder.IBehavior(rep.incMap(fa)(fb)(accumulator), graph)

  def map2[B, DeltaB, C, DeltaC](b: T#IBehavior[B, DeltaB])(
      valueFun: (A, B) => C)(
      deltaFun: (A, B, Ior[DeltaA, DeltaB]) => Option[DeltaC])(
      foldFun: (C, DeltaC) => C): T#IBehavior[C, DeltaC] =
    builder.IBehavior(
      rep.incMap2(b.rep)(valueFun)(deltaFun)(foldFun),
      GraphState.any.combine(graph, b.graph)
    )

  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    builder.event(
      core.IBehavior.syntaxSnapshottable(rep).snapshotWith(ev.rep)(f),
      ev.graph.mergeGraphAndEffect(this.graph))

  def toDBehavior: T#DBehavior[A] =
    builder.DBehavior(rep.toDBehavior, graph)
}

abstract class HokkoIBehaviorObject[
    SubT <: HokkoTier { type T = SubT }: HokkoBuilder]
    extends IBehaviorObject[SubT] {
  private[this] val hokkoBuilder = implicitly[HokkoBuilder[SubT]]

  def constant[A, B](x: A): SubT#IBehavior[A, B] =
    hokkoBuilder.IBehavior(core.IBehavior.constant(x), GraphState.default)
}
