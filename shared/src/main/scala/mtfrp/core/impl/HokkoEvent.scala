package mtfrp.core.impl

import hokko.core
import mtfrp.core._

private[core] class HokkoEvent[T <: HokkoTier: HokkoBuilder, A](
    private[core] val rep: core.Event[A],
    private[core] val graph: ReplicationGraph
) extends Event[T, A] {

  private[this] val hokkoBuilder = implicitly[HokkoBuilder[T]]

  def fold[B, AA >: A](initial: B)(
      f: (B, AA) => B): T#IncrementalBehavior[B, AA] =
    hokkoBuilder.incrementalBehavior(rep.fold(initial)(f), initial, graph, f)

  def unionWith[B, C, AA >: A](b: T#Event[B])(f1: AA => C)(f2: B => C)(
      f3: (AA, B) => C): T#Event[C] =
    hokkoBuilder.event(rep.unionWith(b.rep)(f1)(f2)(f3), graph + b.graph)

  def collect[B, AA >: A](fb: A => Option[B]): T#Event[B] =
    hokkoBuilder.event(rep.collect(fb), graph)
}

abstract class HokkoEventObject[
    SubT <: HokkoTier { type T = SubT }: HokkoBuilder]
    extends EventObject[SubT] {
  val builder = implicitly[HokkoBuilder[SubT]]

  def empty[A]: SubT#Event[A] =
    builder.event(core.Event.empty, ReplicationGraph.start)

  private[core] def apply[A](ev: core.Event[A]): SubT#Event[A] =
    builder.event(ev, ReplicationGraph.start)
}
