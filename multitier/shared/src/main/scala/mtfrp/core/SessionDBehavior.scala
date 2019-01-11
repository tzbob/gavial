package mtfrp.core

import hokko.core.Thunk
import io.circe.{Decoder, Encoder}

class SessionDBehavior[A] private[core] (
    private[core] val underlying: AppDBehavior[Map[Client, A]],
    initialByName: => A,
    graphByName: => GraphState
) extends DBehavior[SessionTier, A] {
  private[core] lazy val initial = initialByName
  private[core] lazy val graph   = graphByName

  override def changes(): SessionEvent[A] =
    new SessionEvent(underlying.changes, graph)

  override def toBehavior: SessionTier#Behavior[A] =
    new SessionBehavior(underlying.toBehavior, graph, Thunk.eager(initial))

  override def toIBehavior[DeltaA](diff: (A, A) => DeltaA)(
      patch: (A, DeltaA) => A): SessionIBehavior[A, DeltaA] = {

    new SessionIBehavior(
      underlying.toIBehavior { (mOld: Map[Client, A], mNow: Map[Client, A]) =>
        val disconnects = (mOld.keySet -- mNow.keys).map(Disconnected.apply)
        val connects    = (mNow.keySet -- mOld.keys).map(Connected.apply)

        val clientChanges: Set[ClientChange] = disconnects ++ connects
        val newMap: Map[Client, DeltaA] = mNow.keySet
          .intersect(mOld.keySet)
          .map { client =>
            client -> diff(mNow(client), mOld(client))
          }
          .toMap

        newMap -> clientChanges
      }(IBehavior.transformFromNormalToSetClientChangeMap(initial, patch)),
      initial,
      graph
    )
  }

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
                         GraphState.any.combine(graph, fb.graph))
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

  override def delayed[A](db: => SessionDBehavior[A]): SessionBehavior[A] = {
    val delayedApp = AppDBehavior.delayed(db.underlying)
    new SessionBehavior[A](delayedApp, delayedApp.graph, Thunk(db.initial))
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

  val client: SessionDBehavior[Client] = {
    val clientsApp = AppDBehavior.clients.map { cs =>
      cs.map(c => c -> c).toMap
    }
    new SessionDBehavior(clientsApp, ClientGenerator.fresh, clientsApp.graph.ws)
  }
}
