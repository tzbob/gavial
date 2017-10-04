package mtfrp.core.session

import mtfrp.core._

class SessionBehavior[A] private[session] (
    val underlying: AppBehavior[Client => A]
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

  val client: SessionBehavior[Client] =
    new SessionBehavior(AppBehavior.constant(identity[Client] _))
}
