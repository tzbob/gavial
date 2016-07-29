package mtfrp
package core

trait Event[T <: Tier, A] {
  private[core] val graph: ReplicationGraph

  def fold[B, AA >: A](initial: B)(f: (B, AA) => B): T#IncrementalBehavior[B, AA]

  def unionWith[B, C, AA >: A](b: T#Event[B])(f1: AA => C)(f2: B => C)(f3: (AA, B) => C): T#Event[C]

  def collect[B, AA >: A](fb: A => Option[B]): T#Event[B]

  // Derived ops

  def hold[AA >: A](initial: AA): T#DiscreteBehavior[AA]

  def unionLeft[AA >: A](other: T#Event[AA]): T#Event[AA]

  def unionRight[AA >: A](other: T#Event[AA]): T#Event[AA]

  def mergeWith[AA >: A](events: T#Event[AA]*): T#Event[Seq[AA]]

  def map[B](f: A => B): T#Event[B]

  def dropIf[B](f: A => Boolean): T#Event[A]
}
