package mtfrp.core

class SessionDiscreteBehavior[A] private[core] (
    val underlying: AppDiscreteBehavior[Client => A]
) extends DiscreteBehavior[SessionTier, A] {
  override def changes(): SessionEvent[A] = {
    val changes = underlying.changes.map { (cf: Client => A) => c: Client =>
      Some(cf(c)): Option[A]
    }
    new SessionEvent(changes)
  }

  override def toBehavior: SessionTier#Behavior[A] =
    new SessionBehavior(underlying.toBehavior)

  override def reverseApply[B](fb: SessionTier#DiscreteBehavior[A => B])
    : SessionTier#DiscreteBehavior[B] = {
    val newFb = fb.underlying.map { f => cf: (Client => A) => c: Client =>
      f(c)(cf(c))
    }
    val revApped = underlying.reverseApply(newFb)
    new SessionDiscreteBehavior(revApped)
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

object SessionDiscreteBehavior extends DiscreteBehaviorObject[SessionTier] {
  override def constant[A](x: A): SessionDiscreteBehavior[A] =
    new SessionDiscreteBehavior(AppDiscreteBehavior.constant((_: Client) => x))

  def toApp[A](sessionBehavior: SessionDiscreteBehavior[A])
    : AppDiscreteBehavior[Map[Client, A]] =
    AppDiscreteBehavior.clients.map2(sessionBehavior.underlying)(
      SessionBehavior.clientMerger)
}
