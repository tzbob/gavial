package mtfrp
package core

trait Tier {
  type T <: Tier.Aux[T]
  type Event[A] <: core.Event[T, A]
  type Behavior[A] <: core.Behavior[T, A]
  type DBehavior[A] <: core.DBehavior[T, A]
  type IBehavior[A, DeltaA] <: core.IBehavior[T, A, DeltaA]

  val Event: core.EventObject[T]
  val Behavior: core.BehaviorObject[T]
  val DBehavior: core.DBehaviorObject[T]
  val IBehavior: core.IBehaviorObject[T]

  val Async: core.AsyncObject[T]

  type Replicated <: Tier
}

object Tier {
  type Aux[A] = Tier { type T = A }

  trait Concrete[T] {
    def tier: Tier.Aux[T]
  }

  def tier[T](implicit ev: Concrete[T]): Tier.Aux[T] =
    ev.tier

  def example[T <: Tier: Concrete]: T#Behavior[Int] = {
    val tier = Tier.tier[T]
    val b    = tier.Behavior.constant(4)

    import tier.Behavior._

    b.map { e =>
      e
    }

    b
  }
}
