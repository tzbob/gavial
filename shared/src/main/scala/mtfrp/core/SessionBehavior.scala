package mtfrp.core

class SessionBehavior[A] private[core] (
    private[core] val underlying: AppBehavior[Client => A]
) extends Behavior[SessionTier, A] {
  override def reverseApply[B](
      fb: SessionTier#Behavior[A => B]): SessionTier#Behavior[B] = {
    val newFb = fb.underlying.map { f => cf: (Client => A) => c: Client =>
      f(c)(cf(c))
    }
    val revApped = underlying.reverseApply(newFb)
    new SessionBehavior(revApped)
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

object SessionBehavior extends BehaviorObject[SessionTier] {
  override def constant[A](x: A): SessionTier#Behavior[A] =
    new SessionBehavior(AppBehavior.constant((_: Client) => x))

  private[core] def clientMerger[A](clients: Set[Client], cfA: Client => A) =
    clients.map { c =>
      c -> cfA(c)
    }.toMap

  def toApp[A](
      sessionBehavior: SessionBehavior[A]): AppBehavior[Map[Client, A]] = {
    AppBehavior.clients.map2(sessionBehavior.underlying)(clientMerger)
  }

  val client: SessionBehavior[Client] =
    new SessionBehavior(AppBehavior.constant(identity[Client] _))
}
