package mtfrp.core.impl

import hokko.core
import mtfrp.core._
import mtfrp.core.mock.MockBuilder

class HokkoBehavior[T <: HokkoTier: HokkoBuilder, A](
    private[core] val rep: core.CBehavior[A],
    private[core] val graph: ReplicationGraph
)(implicit mockBuilder: MockBuilder[T#Replicated])
    extends Behavior[T, A] {

  private[this] val hokkoBuilder = implicitly[HokkoBuilder[T]]

  def reverseApply[B, AA >: A](fb: T#Behavior[(AA) => B]): T#Behavior[B] =
    hokkoBuilder.behavior(fb.rep ap rep, graph + fb.graph)

  def snapshotWith[B, AA >: A, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    hokkoBuilder.event(rep.snapshotWith(ev.rep)(f), graph + ev.graph)
}

abstract class HokkoBehaviorObject[
    SubT <: HokkoTier { type T = SubT }: HokkoBuilder]
    extends BehaviorObject[SubT] {
  private[this] val hokkoBuilder = implicitly[HokkoBuilder[SubT]]
  def constant[A](x: A): SubT#Behavior[A] =
    hokkoBuilder.behavior(core.CBehavior.constant(x), ReplicationGraph.start)
}
