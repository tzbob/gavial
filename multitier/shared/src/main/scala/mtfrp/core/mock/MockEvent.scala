package mtfrp.core.mock

import hokko.{core => HC}
import mtfrp.core._

class MockEvent[T <: MockTier: MockBuilder, A](
    private[core] val graph: GraphState
) extends Event[T, A] {

  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def foldI[B](initial: B)(f: (B, A) => B): T#IBehavior[B, A] =
    mockBuilder.IBehavior(graph, f, initial)

  def unionWith(b: T#Event[A])(f3: (A, A) => A): T#Event[A] =
    mockBuilder.event(GraphState.any.combine(graph, b.graph))

  def collect[B](fb: A => Option[B]): T#Event[B] =
    mockBuilder.event(graph)
}

abstract class MockEventObject[SubT <: MockTier { type T = SubT }: MockBuilder]
    extends EventObject[SubT] {
  val builder = implicitly[MockBuilder[SubT]]

  def empty[A]: SubT#Event[A] = builder.event(GraphState.default)

  private[core] def apply[A](ev: HC.Event[A],
                             graphState: GraphState): SubT#Event[A] =
    builder.event(graphState)
}
