package mtfrp.core.impl

import hokko.core
import mtfrp.core._
import mtfrp.core.mock.MockBuilder

class HokkoDiscreteBehavior[T <: HokkoTier: HokkoBuilder, A](
    private[core] val rep: core.DBehavior[A],
    private[core] val initial: A,
    private[core] val graph: ReplicationGraph
)(implicit mockBuilder: MockBuilder[T#Replicated])
    extends DiscreteBehavior[T, A] {

  private[this] val builder = implicitly[HokkoBuilder[T]]

  def changes(): T#Event[A] =
    builder.event(rep.changes(), graph)

  def reverseApply[B, AA >: A](
      fb: T#DiscreteBehavior[AA => B]): T#DiscreteBehavior[B] =
    builder.discreteBehavior(fb.rep ap this.rep,
                             fb.initial(this.initial),
                             graph + fb.graph)

  def snapshotWith[B, AA >: A, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    builder.event(this.rep.snapshotWith(ev.rep)(f), graph + ev.graph)

  def toBehavior: T#Behavior[A] =
    builder.behavior(rep.toCBehavior, graph)

}

abstract class HokkoDiscreteBehaviorObject[
    SubT <: HokkoTier { type T = SubT }: HokkoBuilder]
    extends DiscreteBehaviorObject[SubT] {
  private[this] val hokkoBuilder = implicitly[HokkoBuilder[SubT]]

  def constant[A](x: A): SubT#DiscreteBehavior[A] =
    hokkoBuilder
      .discreteBehavior(core.DBehavior.constant(x), x, ReplicationGraph.start)
}
