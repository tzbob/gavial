package mtfrp
package core

import hokko.{core => HC}
import io.circe._

class MockIncBehavior[T <: MockTier: MockBuilder, A, DeltaA](
    graph: ReplicationGraph,
    private[core] val accumulator: (A, DeltaA) => A,
    initial: A
)(implicit hokkoBuilder: HokkoBuilder[T#Replicated])
    extends MockDiscreteBehavior[T, A](graph, initial)
    with IncrementalBehavior[T, A, DeltaA] {

  private[this] val builder = implicitly[MockBuilder[T]]

  def deltas: T#Event[DeltaA] =
    builder.event(graph)

  def map[B, DeltaB](accumulator: (B, DeltaB) => B)(fa: A => B)(
      fb: DeltaA => DeltaB): T#IncrementalBehavior[B, DeltaB] =
    builder.incrementalBehavior(graph, accumulator, fa(initial))
}
