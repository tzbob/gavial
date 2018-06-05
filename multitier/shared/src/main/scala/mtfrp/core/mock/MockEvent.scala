package mtfrp.core.mock

import hokko.{core => HC}
import mtfrp.core._

class MockEvent[T <: MockTier: MockBuilder, A](
    private[core] val graph: ReplicationGraph,
    private[core] val requiresWebSockets: Boolean
) extends Event[T, A] {

  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def fold[B](initial: B)(f: (B, A) => B): T#IBehavior[B, A] =
    mockBuilder.IBehavior(graph, f, initial, requiresWebSockets)

  def unionWith(b: T#Event[A])(f3: (A, A) => A): T#Event[A] =
    mockBuilder.event(graph + b.graph,
                      requiresWebSockets || b.requiresWebSockets)

  def collect[B](fb: A => Option[B]): T#Event[B] =
    mockBuilder.event(graph, requiresWebSockets)
}

abstract class MockEventObject[SubT <: MockTier { type T = SubT }: MockBuilder]
    extends EventObject[SubT] {
  val builder = implicitly[MockBuilder[SubT]]

  def empty[A]: SubT#Event[A] = builder.event(ReplicationGraph.start, false)

  private[core] def apply[A](ev: HC.Event[A], requiresWebSocket: Boolean)
  : SubT#Event[A] =
    builder.event(ReplicationGraph.start, requiresWebSocket)
}
