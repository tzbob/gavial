package mtfrp.core

class SessionDBehavior[A] private[core](
    val underlying: AppDBehavior[Client => A]
) extends DBehavior[SessionTier, A] {
  override def changes(): SessionEvent[A] = {
    val changes = underlying.changes.map { (cf: Client => A) => c: Client =>
      Some(cf(c)): Option[A]
    }
    new SessionEvent(changes)
  }

  override def toBehavior: SessionTier#Behavior[A] =
    new SessionBehavior(underlying.toBehavior)

  override def reverseApply[B](fb: SessionTier#DBehavior[A => B])
    : SessionTier#DBehavior[B] = {
    val newFb = fb.underlying.map { f => cf: (Client => A) => c: Client =>
      f(c)(cf(c))
    }
    val revApped = underlying.reverseApply(newFb)
    new SessionDBehavior(revApped)
  }

  override def snapshotWith[B, C](ev: SessionEvent[B])(
      f: (A, B) => C): SessionEvent[C] = {
    val newUnder = underlying.snapshotWith(ev.underlying) {
      (cfA: Client => A, cfB: Client => Option[B]) => c: Client =>
        val maybeB = cfB(c)
        maybeB.map { b =>
          f(cfA(c), b)
        }
    }
    new SessionEvent(newUnder)
  }
}

object SessionDBehavior extends DBehaviorObject[SessionTier] {
  override def constant[A](x: A): SessionDBehavior[A] =
    new SessionDBehavior(AppDBehavior.constant((_: Client) => x))

  def toApp[A](sessionBehavior: SessionDBehavior[A])
    : AppDBehavior[Map[Client, A]] =
    AppDBehavior.clients.map2(sessionBehavior.underlying)(
      SessionBehavior.clientMerger)
}
