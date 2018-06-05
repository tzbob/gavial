package mtfrp.core.impl

import hokko.core
import mtfrp.core._

class HokkoDBehavior[T <: HokkoTier: HokkoBuilder, A](
    private[core] val rep: core.DBehavior[A],
    private[core] val initial: A,
    private[core] val graph: ReplicationGraph,
    private[core] val requiresWebSockets: Boolean
) extends DBehavior[T, A] {

  private[this] val builder = implicitly[HokkoBuilder[T]]

  def changes(): T#Event[A] =
    builder.event(rep.changes(), graph, requiresWebSockets)

  def reverseApply[B](fb: T#DBehavior[A => B]): T#DBehavior[B] =
    builder.DBehavior(fb.rep ap this.rep,
                      fb.initial(this.initial),
                      graph + fb.graph,
                      requiresWebSockets || fb.requiresWebSockets)

  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    builder.event(this.rep.snapshotWith(ev.rep)(f),
                  graph + ev.graph,
                  ev.requiresWebSockets)

  def toBehavior: T#Behavior[A] =
    builder.behavior(rep.toCBehavior, graph, requiresWebSockets)

}

abstract class HokkoDBehaviorObject[
    SubT <: HokkoTier { type T = SubT }: HokkoBuilder]
    extends DBehaviorObject[SubT] {
  private[this] val hokkoBuilder = implicitly[HokkoBuilder[SubT]]

  def constant[A](x: A): SubT#DBehavior[A] =
    hokkoBuilder
      .DBehavior(core.DBehavior.constant(x), x, ReplicationGraph.start, false)
}