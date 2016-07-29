package mtfrp
package core

trait DiscreteBehavior[T <: Tier, A] extends Behavior[T, A] {
  private[core] val initial: A

  def changes(): T#Event[A]

  def discreteReverseApply[B, AA >: A](fb: T#DiscreteBehavior[A => B]): T#DiscreteBehavior[B]

  def withDeltas[DeltaA, AA >: A](init: AA, deltas: T#Event[DeltaA]): T#IncrementalBehavior[AA, DeltaA]

  // derived ops

  def discreteMap[B](f: A => B): T#DiscreteBehavior[B]

  def discreteMap2[B, C](b: T#DiscreteBehavior[B])(f: (A, B) => C): T#DiscreteBehavior[C]

  def discreteMap3[B, C, D](b: T#DiscreteBehavior[B], c: T#DiscreteBehavior[C])(f: (A, B, C) => D): T#DiscreteBehavior[D]
}
