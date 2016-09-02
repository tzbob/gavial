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
  private[core] def transformToNormal[X, Y](
      f: (Client => X, Client => Option[Y]) => (Client => X)
  ): (X, Y) => X = { (x, y) =>
    val xf      = (_: Client) => x
    val yf      = (_: Client) => (Some(y): Option[Y])
    val resultF = f(xf, yf)
    resultF(null) // this is fine | we want this to blow up if it gets used
  }

  private[core] def transformFromNormal[A, DeltaA](f: (A, DeltaA) => A)
    : (Map[Client, A], (Client, DeltaA)) => Map[Client, A] = {
    (acc: Map[Client, A], delta: (Client, DeltaA)) =>
      delta match {
        case (client, clientDelta) =>
          // FIXME: relying on Map.default is dangerous
          val clientAcc = acc.getOrElse(client, acc.default(client))
          acc.updated(client, f(clientAcc, clientDelta))
      }
  }

}
