package mtfrp
package core

trait IncrementalBehavior[T <: Tier, A, DeltaA]
    extends DiscreteBehavior[T, A] {

  private[core] def accumulator: (A, DeltaA) => A

  def deltas: T#Event[DeltaA]
  def map[B, DeltaB](accumulator: (B, DeltaB) => B)(fa: A => B)(
      fb: DeltaA => DeltaB): T#IncrementalBehavior[B, DeltaB]
}

object IncrementalBehavior {
    private[core] def transform[X, Y](
        f: (Client => X, Client => Option[Y]) => (Client => X)
    ): (X, Y) => X = { (x, y) =>
      val xf      = (_: Client) => x
      val yf      = (_: Client) => (Some(y): Option[Y])
      val resultF = f(xf, yf)
      resultF(null) // this is fine | we want this to blow up if it gets used
    }
}
