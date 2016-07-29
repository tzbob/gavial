package mtfrp
package core

import hokko.core
import io.circe._

class HokkoIncBehavior[T <: HokkoTier: HokkoBuilder, A, DeltaA](
  private[core] override val rep: core.IncrementalBehavior[A, DeltaA],
  initial: A,
  override private[core] val graph: ReplicationGraph
)(implicit mockBuilder: MockBuilder[T#Replicated])
  extends HokkoDiscreteBehavior[T, A](rep, initial, graph)
  with IncrementalBehavior[T, A, DeltaA] {

  private[this] val builder = implicitly[HokkoBuilder[T]]

  def deltas: T#Event[DeltaA] =
    builder.event(rep.deltas, graph)

  def map[B, DeltaB](accumulator: (B, DeltaB) => B)(fa: A => B)(fb: DeltaA => DeltaB): T#IncrementalBehavior[B, DeltaB] =
    builder.incrementalBehavior(rep.map(accumulator)(fa)(fb), fa(initial), graph)
}
