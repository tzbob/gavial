package mtfrp.core

import cats.implicits._

class SessionBehavior[A] private[core] (
    private[core] val underlying: AppBehavior[Map[Client, A]],
    private[core] val requiresWebSockets: Boolean
) extends Behavior[SessionTier, A] {

  override def reverseApply[B](
      fb: SessionTier#Behavior[A => B]): SessionTier#Behavior[B] = {

    val mapFb: AppBehavior[Map[Client, A] => Map[Client, B]] =
      fb.underlying.map {
        (mapF: Map[Client, A => B]) => mapArg: Map[Client, A] =>
          mapF ap mapArg
      }

    val revApped = underlying.reverseApply(mapFb)
    new SessionBehavior(revApped,
                        this.requiresWebSockets || fb.requiresWebSockets)
  }

  override def snapshotWith[B, C](ev: SessionEvent[B])(
      f: (A, B) => C): SessionEvent[C] = {
    val newUnder = underlying.snapshotWith(ev.underlying) {
      (cfA: Map[Client, A], cfB: Map[Client, B]) =>
        cfA.map2(cfB)(f)
    }
    new SessionEvent(newUnder, ev.requiresWebSockets)
  }
}

object SessionBehavior extends BehaviorObject[SessionTier] {
  override def constant[A](x: A): SessionTier#Behavior[A] = {
    val clientMap = AppBehavior.clients.map { clients =>
      clients.map(_ -> x).toMap
    }
    new SessionBehavior(clientMap, false)
  }

  def toApp[A](sb: SessionBehavior[A]): AppBehavior[Map[Client, A]] =
    sb.underlying

  val client: SessionBehavior[Client] = {
    val clientMap = AppBehavior.clients.map { clients =>
      clients.map(c => c -> c).toMap
    }
    new SessionBehavior(clientMap, true)
  }
}
