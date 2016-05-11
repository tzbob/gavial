package mtfrp
package core

trait Behavior[T <: Tier, A] {
  def reverseApply[B, AA >: A](fb: T#Behavior[AA => B]): T#Behavior[B]

  def snapshotWith[B, AA >: A](ev: T#Event[AA => B]): T#Event[B]

  // Derived ops

  def map[B](f: A => B): T#Behavior[B]

  def map2[B, C](b: T#Behavior[B])(f: (A, B) => C): T#Behavior[C]

  def map3[B, C, D](b: T#Behavior[B], c: T#Behavior[C])(f: (A, B, C) => D): T#Behavior[D]

  def sampledBy(ev: T#Event[_]): T#Event[A]
}

trait BehaviorObject[T <: Tier] {
  def constant[A](x: A): T#Behavior[A]
}
