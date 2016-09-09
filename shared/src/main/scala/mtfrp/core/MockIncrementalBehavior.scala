package mtfrp
package core

class MockIncBehavior[T <: MockTier: MockBuilder, A, DeltaA](
    private[core] val graph: ReplicationGraph,
    private[core] val accumulator: (A, DeltaA) => A,
    private[core] val initial: A
)(implicit hokkoBuilder: HokkoBuilder[T#Replicated])
    extends IncrementalBehavior[T, A, DeltaA] {

  private[this] val builder = implicitly[MockBuilder[T]]

  def changes: T#Event[A] = builder.event(graph)

  def deltas: T#Event[DeltaA] = builder.event(graph)

  def map[B, DeltaB](accumulator: (B, DeltaB) => B)(fa: A => B)(
      fb: DeltaA => DeltaB): T#IncrementalBehavior[B, DeltaB] =
    builder.incrementalBehavior(graph, accumulator, fa(initial))

  def snapshotWith[B, AA >: A, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    builder.event(graph + ev.graph)

  def toDiscreteBehavior: T#DiscreteBehavior[A] =
    builder.discreteBehavior(graph, initial)
}

abstract class MockIncrementalBehaviorObject[
    SubT <: MockTier { type T = SubT }: MockBuilder]
    extends IncrementalBehaviorObject[SubT] {
  private[this] val mockBuilder = implicitly[MockBuilder[SubT]]

  def constant[A](x: A): SubT#IncrementalBehavior[A, Nothing] =
    mockBuilder
      .incrementalBehavior(ReplicationGraph.start, (a: A, _: Any) => a, x)
}
