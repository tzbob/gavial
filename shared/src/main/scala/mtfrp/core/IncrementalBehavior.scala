package mtfrp
package core

trait IncrementalBehavior[T <: Tier, A, DeltaA]
    extends DiscreteBehavior[T, A] {

  private[core] def accumulator: (A, DeltaA) => A

  def deltas: T#Event[DeltaA]
  def map[B, DeltaB](accumulator: (B, DeltaB) => B)(fa: A => B)(
      fb: DeltaA => DeltaB): T#IncrementalBehavior[B, DeltaB]
}
