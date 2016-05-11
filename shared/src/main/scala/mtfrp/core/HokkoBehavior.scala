package mtfrp
package core

import hokko.core

class HokkoBehavior[T <: HokkoTier: HokkoBuilder, A](
  private[core] val rep: core.Behavior[A],
  private[core] val graph: ReplicationGraph
)(implicit mockBuilder: MockBuilder[T#Replicated]) extends Behavior[T, A] {

  private[this] val hokkoBuilder = implicitly[HokkoBuilder[T]]

  def map[B](f: A => B): T#Behavior[B] =
    hokkoBuilder.behavior(rep.map(f), graph)

  def map2[B, C](b: T#Behavior[B])(f: (A, B) => C): T#Behavior[C] =
    hokkoBuilder.behavior(rep.map2(b.rep)(f), graph + b.graph)

  def map3[B, C, D](b: T#Behavior[B], c: T#Behavior[C])(f: (A, B, C) => D): T#Behavior[D] =
    hokkoBuilder.behavior(rep.map3(b.rep, c.rep)(f), graph + c.graph + b.graph)

  def reverseApply[B, AA >: A](fb: T#Behavior[AA => B]): T#Behavior[B] =
    hokkoBuilder.behavior(rep.reverseApply(fb.rep), graph + fb.graph)

  def sampledBy(ev: T#Event[_]): T#Event[A] =
    hokkoBuilder.event(rep.sampledBy(ev.rep), graph + ev.graph)

  def snapshotWith[B, AA >: A](ev: T#Event[AA => B]): T#Event[B] =
    hokkoBuilder.event(rep.snapshotWith(ev.rep), graph + ev.graph)
}

abstract class HokkoBehaviorOps[T <: HokkoTier: HokkoBuilder] {
  private[this] val hokkoBuilder = implicitly[HokkoBuilder[T]]

  def constant[A](x: A): T#Behavior[A] =
    hokkoBuilder.behavior(core.Behavior.constant(x), ReplicationGraph.start)
}
