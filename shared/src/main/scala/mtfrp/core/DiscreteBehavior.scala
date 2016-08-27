package mtfrp
package core

trait DiscreteBehavior[T <: Tier, A] extends Behavior[T, A] {

  private[core] def initial: A

  def changes(): T#Event[A]

  def discreteReverseApply[B, AA >: A](
      fb: T#DiscreteBehavior[A => B]): T#DiscreteBehavior[B]

  // derived ops

  def discreteMap[B](f: A => B): T#DiscreteBehavior[B]

  def discreteMap2[B, C](b: T#DiscreteBehavior[B])(
      f: (A, B) => C): T#DiscreteBehavior[C]

  def discreteMap3[B, C, D](
      b: T#DiscreteBehavior[B],
      c: T#DiscreteBehavior[C])(f: (A, B, C) => D): T#DiscreteBehavior[D]
}

trait DiscreteBehaviorObject[T <: Tier] {
  def constant[A](x: A): T#DiscreteBehavior[A]
}
