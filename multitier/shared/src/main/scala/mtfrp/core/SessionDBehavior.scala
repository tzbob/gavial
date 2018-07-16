package mtfrp.core

import io.circe.{Decoder, Encoder}

class SessionDBehavior[A] private[core] (
    private[core] val underlying: AppDBehavior[Map[Client, A]],
    private[core] val initial: A,
    private[core] val graph: GraphState
) extends DBehavior[SessionTier, A] {
  override def changes(): SessionEvent[A] =
    new SessionEvent(underlying.changes, graph)

  override def toBehavior: SessionTier#Behavior[A] =
    new SessionBehavior(underlying.toBehavior, graph)

  override def toIBehavior[DeltaA](diff: (A, A) => DeltaA)(
      patch: (A, DeltaA) => A): SessionIBehavior[A, DeltaA] =
    new SessionIBehavior(
      underlying.toIBehavior {
        (mapOld: Map[Client, A], mapNow: Map[Client, A]) =>
          val keys = mapOld.keySet intersect mapNow.keySet
          keys.map { key =>
            key -> diff(mapOld(key), mapNow(key))
          }.toMap
      } { (map, deltaMap) =>
        val keys = map.keySet intersect deltaMap.keySet
        keys.map { key =>
          key -> patch(map(key), deltaMap(key))
        }.toMap
      },
      initial,
      graph
    )

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
    new SessionDBehavior(revApped,
                         fb.initial(initial),
                         GraphState.all.combine(graph, fb.graph))
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

    new SessionEvent(newUnder, ev.graph.mergeGraphAndEffect(this.graph))
  }
}

object SessionDBehavior extends DBehaviorObject[SessionTier] {
  override def constant[A](x: A): SessionDBehavior[A] = {
    val clientMap = AppDBehavior.clients.map { clients =>
      clients.map(_ -> x).toMap
    }
    val state = clientMap.graph.xhr
    new SessionDBehavior(clientMap, x, state)
  }

  override def delayed[A](db: => SessionDBehavior[A],
                          init: A): SessionDBehavior[A] = {
    val delayedApp = AppDBehavior.delayed(
      db.underlying,
      Map.empty[Client, A].withDefaultValue(init))
    new SessionDBehavior[A](delayedApp, init, delayedApp.graph)
  }

  def toApp[A](sb: SessionDBehavior[A]): AppDBehavior[Map[Client, A]] =
    sb.underlying

  def toClient[A, DeltaA](sessionB: SessionDBehavior[A])(
      implicit dec: Decoder[A],
      enc: Encoder[A]): ClientDBehavior[A] = {
    SessionIBehavior
      .toClient(sessionB.toIBehavior((_, a) => a)((_, a) => a))
      .toDBehavior
  }

}
