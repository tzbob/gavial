package mtfrp.core.impl

import hokko.core
import mtfrp.core._

class HokkoDBehavior[T <: HokkoTier: HokkoBuilder, A](
    private[core] val rep: core.DBehavior[A],
    private[core] val initial: A,
    graphByName: => GraphState
) extends DBehavior[T, A] {
  private[core] lazy val graph = graphByName

  private[this] val builder = implicitly[HokkoBuilder[T]]

  def changes(): T#Event[A] =
    builder.event(rep.changes(), graph)

  def reverseApply[B](fb: T#DBehavior[A => B]): T#DBehavior[B] =
    builder.DBehavior(fb.rep ap this.rep,
                      fb.initial(this.initial),
                      GraphState.any.combine(graph, fb.graph))

  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    builder.event(this.rep.snapshotWith(ev.rep)(f),
                  ev.graph.mergeGraphAndEffect(this.graph))

  def toBehavior: T#Behavior[A] = builder.behavior(rep.toCBehavior, graph)

  def toIBehavior[DeltaA](diff: (A, A) => DeltaA)(
      patch: (A, DeltaA) => A): T#IBehavior[A, DeltaA] =
    builder.IBehavior(rep.toIBehavior(diff)(patch), initial, graphByName, patch)
}

abstract class HokkoDBehaviorObject[
    SubT <: HokkoTier { type T = SubT }: HokkoBuilder]
    extends DBehaviorObject[SubT] {
  private[this] val hokkoBuilder = implicitly[HokkoBuilder[SubT]]
  def constant[A](x: A): SubT#DBehavior[A] =
    hokkoBuilder.DBehavior(core.DBehavior.constant(x), x, GraphState.default)

  def delayed[A](db: => SubT#DBehavior[A], init: A): SubT#DBehavior[A] =
    hokkoBuilder.DBehavior(core.DBehavior.delayed(db.rep, init),
                           init,
                           GraphState.delayed(db.graph))
}
