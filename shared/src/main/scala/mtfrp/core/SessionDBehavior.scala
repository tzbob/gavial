package mtfrp.core

class SessionDBehavior[A] private[core] (
    val underlying: AppDBehavior[Map[Client, A]]
) extends DBehavior[SessionTier, A] {
  override def changes(): SessionEvent[A] =
    new SessionEvent(underlying.changes)

  override def toBehavior: SessionTier#Behavior[A] =
    new SessionBehavior(underlying.toBehavior)

  override def reverseApply[B](
      fb: SessionTier#DBehavior[A => B]): SessionTier#DBehavior[B] = {
    val mapFb: AppDBehavior[Map[Client, A] => Map[Client, B]] =
      fb.underlying.map {
        (mapF: Map[Client, A => B]) => mapArg: Map[Client, A] =>
          val commonClients = mapF.keySet intersect mapArg.keySet

          commonClients.map { client =>
            client -> mapF(client)(mapArg(client))
          }.toMap
      }

    val revApped = underlying.reverseApply(mapFb)
    new SessionDBehavior(revApped)
  }

  override def snapshotWith[B, C](ev: SessionEvent[B])(
      f: (A, B) => C): SessionEvent[C] = {
    val newUnder = underlying.snapshotWith(ev.underlying) {
      (cfA: Map[Client, A], cfB: Map[Client, B]) =>
        val commonClients = cfA.keySet intersect cfB.keySet
        commonClients.map { client =>
          client -> f(cfA(client), cfB(client))
        }.toMap
    }
    new SessionEvent(newUnder)
  }
}

object SessionDBehavior extends DBehaviorObject[SessionTier] {
  override def constant[A](x: A): SessionDBehavior[A] = {
    val clientMap = AppDBehavior.clients.map { clients =>
      clients.map(_ -> x).toMap
    }
    new SessionDBehavior(clientMap)
  }

  def toApp[A](sb: SessionDBehavior[A]): AppDBehavior[Map[Client, A]] =
    sb.underlying
}
