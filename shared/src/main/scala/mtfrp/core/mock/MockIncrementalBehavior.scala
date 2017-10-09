package mtfrp.core.mock

import cats.data.Ior
import mtfrp.core._
import mtfrp.core.impl.HokkoBuilder

class MockIncBehavior[T <: MockTier: MockBuilder, A, DeltaA](
    private[core] val graph: ReplicationGraph,
    private[core] val accumulator: (A, DeltaA) => A,
    private[core] val initial: A
)(implicit hokkoBuilder: HokkoBuilder[T#Replicated])
    extends IncrementalBehavior[T, A, DeltaA] {

  private[this] val builder = implicitly[MockBuilder[T]]

  def changes: T#Event[A] = builder.event(graph)

  def deltas: T#Event[DeltaA] = builder.event(graph)

  def map[B, DeltaB](fa: A => B)(fb: DeltaA => DeltaB)(
      accumulator: (B, DeltaB) => B): T#IncrementalBehavior[B, DeltaB] =
    builder.incrementalBehavior(graph, accumulator, fa(initial))

  def map2[B, DeltaB, C, DeltaC](b: T#IncrementalBehavior[B, DeltaB])(
      valueFun: (A, B) => C)(
      deltaFun: (A, B, Ior[DeltaA, DeltaB]) => Option[DeltaC])(
      foldFun: (C, DeltaC) => C): T#IncrementalBehavior[C, DeltaC] =
    builder.incrementalBehavior(graph, foldFun, valueFun(initial, b.initial))

  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    builder.event(graph + ev.graph)

  def toDiscreteBehavior: T#DiscreteBehavior[A] =
    builder.discreteBehavior(graph, initial)

}

abstract class MockIncrementalBehaviorObject[
    SubT <: MockTier { type T = SubT }: MockBuilder]
    extends IncrementalBehaviorObject[SubT] {
  private[this] val mockBuilder = implicitly[MockBuilder[SubT]]

  def constant[A, B](x: A): SubT#IncrementalBehavior[A, B] =
    mockBuilder
      .incrementalBehavior(ReplicationGraph.start, (a: A, _: Any) => a, x)
}
