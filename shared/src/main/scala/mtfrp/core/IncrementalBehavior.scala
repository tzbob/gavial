package mtfrp
package core

trait IncrementalBehavior[T <: Tier, A, DeltaA] extends DiscreteBehavior[T, A] {
  def deltas: T#Event[DeltaA]
  def map[B, DeltaB](accumulator: (B, DeltaB) => B)(fa: A => B)(fb: DeltaA => DeltaB): T#IncrementalBehavior[B, DeltaB]

  def replicateIncremental: T#Replicated#IncrementalBehavior[A, DeltaA] = ???
}
