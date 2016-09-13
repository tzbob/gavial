package mtfrp.core.session

import mtfrp.core._

class SessionIncrementalBehavior[A, DeltaA] private[session] (
    val underlying: AppIncBehavior[Client => A, Client => Option[DeltaA]]
) extends IncrementalBehavior[SessionTier, A, DeltaA] {
  private[core] def accumulator: (A, DeltaA) => A =
    IncrementalBehavior.transformToNormal(underlying.accumulator)

  def changes: SessionTier#Event[A] = {
    val optChanges = underlying.changes.map { cf => c: Client =>
      Some(cf(c)): Option[A]
    }
    new SessionEvent(optChanges)
  }

  def deltas: SessionTier#Event[DeltaA] = new SessionEvent(underlying.deltas)

  def map[B, DeltaB](accumulator: (B, DeltaB) => B)(fa: A => B)(
      fb: DeltaA => DeltaB): SessionTier#IncrementalBehavior[B, DeltaB] = {

    val newUnderlying: AppIncBehavior[Client => B, Client => Option[DeltaB]] =
      underlying.map {
        (accF: (Client => B), newF: (Client => Option[DeltaB])) => c: Client =>
          newF(c) match {
            case Some(newDelta) => accumulator(accF(c), newDelta)
            case None           => accF(c)
          }
      } { (cf: (Client => A)) => c: Client =>
        fa(cf(c))
      } { (cf: (Client => Option[DeltaA])) => c: Client =>
        cf(c).map(fb)
      }

    new SessionIncrementalBehavior(newUnderlying)
  }

  def snapshotWith[B, C](ev: SessionTier#Event[B])(
      f: (A, B) => C): SessionTier#Event[C] =
    new SessionEvent(
      underlying.snapshotWith(ev.underlying) {
        (af: Client => A, bf: Client => Option[B]) => c: Client =>
          bf(c).map { b =>
            f(af(c), b)
          }
      }
    )

  def toDiscreteBehavior: SessionTier#DiscreteBehavior[A] =
    new SessionDiscreteBehavior(underlying.toDiscreteBehavior)
}

object SessionIncrementalBehavior
    extends IncrementalBehaviorObject[SessionTier] {
  override def constant[A, B](x: A): SessionIncrementalBehavior[A, B] = {
    val src = AppEvent.empty[Client => Option[B]]
    val inc: AppIncBehavior[Client => A, Client => Option[B]] =
      src.fold((_: Client) => x) { (f, _) =>
        f
      }
    new SessionIncrementalBehavior(inc)
  }
}
