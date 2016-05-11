package mtfrp
package core

import hokko.core

class HokkoDiscreteBehavior[T <: HokkoTier: HokkoBuilder, A](
  private[core] override val rep: core.DiscreteBehavior[A],
  val initial: A,
  override private[core] val graph: ReplicationGraph
)(implicit mockBuilder: MockBuilder[T#Replicated])
  extends HokkoBehavior[T, A](rep, graph)
  with DiscreteBehavior[T, A] {

  private[this] val builder = implicitly[HokkoBuilder[T]]

  def changes(): T#Event[A] =
    builder.event(rep.changes(), graph)

  def discreteMap[B](f: A => B): T#DiscreteBehavior[B] =
    builder.discreteBehavior(rep.map(f), f(initial), graph)

  def discreteMap2[B, C](b: T#DiscreteBehavior[B])(f: (A, B) => C): T#DiscreteBehavior[C] =
    builder.discreteBehavior(rep.discreteMap2(b.rep)(f), f(initial, b.initial), graph)

  def discreteMap3[B, C, D](b: T#DiscreteBehavior[B], c: T#DiscreteBehavior[C])(f: (A, B, C) => D): T#DiscreteBehavior[D] =
    builder.discreteBehavior(rep.discreteMap3(b.rep, c.rep)(f), f(initial, b.initial, c.initial), graph)

  def discreteReverseApply[B, AA >: A](fb: T#DiscreteBehavior[A => B]): T#DiscreteBehavior[B] =
    builder.discreteBehavior(rep.discreteReverseApply(fb.rep), fb.initial(initial), graph)

  def withDeltas[DeltaA, AA >: A](init: AA, deltas: T#Event[DeltaA]): T#IncrementalBehavior[AA, DeltaA] =
    builder.incrementalBehavior(rep.withDeltas(init, deltas.rep), init, graph)

}

abstract class HokkoDiscreteBehaviorOps[T <: HokkoTier: HokkoBuilder] {
  private[this] val hokkoBuilder = implicitly[HokkoBuilder[T]]

  def constant[A](x: A): T#DiscreteBehavior[A] =
    hokkoBuilder.discreteBehavior(core.DiscreteBehavior.constant(x), x, ReplicationGraph.start)
}
