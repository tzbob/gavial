package mtfrp.core.impl

import hokko.core
import hokko.core.IBehavior
import mtfrp.core._
import mtfrp.core.mock.MockBuilder

class HokkoIncBehavior[T <: HokkoTier: HokkoBuilder, A, DeltaA](
    private[core] val rep: core.IBehavior[A, DeltaA],
    private[core] val initial: A,
    private[core] val graph: ReplicationGraph,
    private[core] val accumulator: (A, DeltaA) => A
)(implicit mockBuilder: MockBuilder[T#Replicated])
    extends IncrementalBehavior[T, A, DeltaA] {

  private[this] val builder = implicitly[HokkoBuilder[T]]

  def changes: T#Event[A] = builder.event(rep.changes, graph)

  def deltas: T#Event[DeltaA] = builder.event(rep.deltas, graph)

  def map[B, DeltaB](fa: A => B)(fb: DeltaA => DeltaB)(
      accumulator: (B, DeltaB) => B): T#IncrementalBehavior[B, DeltaB] =
    builder.incrementalBehavior(
      rep.incMap(fa)(fb)(accumulator),
      fa(initial),
      graph,
      accumulator
    )

  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    builder.event(IBehavior.syntaxSnapshottable(rep).snapshotWith(ev.rep)(f),
                  graph + ev.graph)

  def toDiscreteBehavior: T#DiscreteBehavior[A] =
    builder.discreteBehavior(rep.toDBehavior, initial, graph)
}

abstract class HokkoIncrementalBehaviorObject[
    SubT <: HokkoTier { type T = SubT }: HokkoBuilder]
    extends IncrementalBehaviorObject[SubT] {
  private[this] val hokkoBuilder = implicitly[HokkoBuilder[SubT]]

  def constant[A, B](x: A): SubT#IncrementalBehavior[A, B] =
    hokkoBuilder.incrementalBehavior(IBehavior.constant(x),
                                     x,
                                     ReplicationGraph.start,
                                     (a: A, _: Any) => a)
}
