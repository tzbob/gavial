package mtfrp.core.mock

import cats.data.Ior
import mtfrp.core._

class MockIBehavior[T <: MockTier: MockBuilder, A, DeltaA](
    private[core] val graph: ReplicationGraph,
    private[core] val accumulator: (A, DeltaA) => A,
    private[core] val initial: A,
    private[core] val requiresWebSockets: Boolean
) extends IBehavior[T, A, DeltaA] {

  private[this] val builder = implicitly[MockBuilder[T]]

  def changes: T#Event[A] = builder.event(graph, requiresWebSockets)

  def deltas: T#Event[DeltaA] = builder.event(graph, requiresWebSockets)

  def map[B, DeltaB](fa: A => B)(fb: DeltaA => DeltaB)(
      accumulator: (B, DeltaB) => B): T#IBehavior[B, DeltaB] =
    builder.IBehavior(graph, accumulator, fa(initial), requiresWebSockets)

  def map2[B, DeltaB, C, DeltaC](b: T#IBehavior[B, DeltaB])(
      valueFun: (A, B) => C)(
      deltaFun: (A, B, Ior[DeltaA, DeltaB]) => Option[DeltaC])(
      foldFun: (C, DeltaC) => C): T#IBehavior[C, DeltaC] =
    builder.IBehavior(graph,
                      foldFun,
                      valueFun(initial, b.initial),
                      requiresWebSockets || b.requiresWebSockets)

  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    builder.event(graph + ev.graph, ev.requiresWebSockets)

  def toDBehavior: T#DBehavior[A] =
    builder.DBehavior(graph, initial, requiresWebSockets)
}

abstract class MockIBehaviorObject[
    SubT <: MockTier { type T = SubT }: MockBuilder]
    extends IBehaviorObject[SubT] {
  private[this] val mockBuilder = implicitly[MockBuilder[SubT]]

  def constant[A, B](x: A): SubT#IBehavior[A, B] =
    mockBuilder
      .IBehavior(ReplicationGraph.start, (a: A, _: Any) => a, x, false)
}