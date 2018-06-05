package mtfrp.core.impl

import hokko.core
import mtfrp.core._

class HokkoEvent[T <: HokkoTier: HokkoBuilder, A](
    private[core] val rep: core.Event[A],
    private[core] val graph: ReplicationGraph,
    private[core] val requiresWebSockets: Boolean
) extends Event[T, A] {

  private[this] val hokkoBuilder = implicitly[HokkoBuilder[T]]

  def fold[B](initial: B)(f: (B, A) => B): T#IBehavior[B, A] =
    hokkoBuilder.IBehavior(rep.fold(initial)(f),
                           initial,
                           graph,
                           f,
                           requiresWebSockets)

  def unionWith(other: T#Event[A])(f: (A, A) => A): T#Event[A] =
    hokkoBuilder.event(rep.unionWith(other.rep)(f),
                       graph + other.graph,
                       requiresWebSockets || other.requiresWebSockets)

  def collect[B](fb: A => Option[B]): T#Event[B] =
    hokkoBuilder.event(rep.collect(fb), graph, requiresWebSockets)
}

abstract class HokkoEventObject[
    SubT <: HokkoTier { type T = SubT }: HokkoBuilder]
    extends EventObject[SubT] {
  val builder = implicitly[HokkoBuilder[SubT]]

  def empty[A]: SubT#Event[A] =
    builder.event(core.Event.empty, ReplicationGraph.start, false)

  private[core] def apply[A](ev: core.Event[A], requiresWebSockets: Boolean)
  : SubT#Event[A] =
    builder.event(ev, ReplicationGraph.start, requiresWebSockets)
}
