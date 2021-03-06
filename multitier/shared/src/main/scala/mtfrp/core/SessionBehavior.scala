package mtfrp.core

import cats.implicits._
import hokko.core.Thunk

class SessionBehavior[A] private[core] (
    private[core] val underlying: AppBehavior[Map[Client, A]],
    graphByName: => GraphState,
    private[core] val initial: Thunk[A]
) extends Behavior[SessionTier, A] {
  private[core] val graph = graphByName

  override def reverseApply[B](
      fb: SessionTier#Behavior[A => B]): SessionTier#Behavior[B] = {

    val mapFb: AppBehavior[Map[Client, A] => Map[Client, B]] =
      fb.underlying.map {
        (mapF: Map[Client, A => B]) => mapArg: Map[Client, A] =>
          mapF ap mapArg
      }

    val revApped = underlying.reverseApply(mapFb)
    new SessionBehavior(revApped,
                        GraphState.any.combine(this.graph, fb.graph),
                        initial.flatMap(a => fb.initial.map(f => f(a))))
  }

  override def snapshotWith[B, C](ev: SessionEvent[B])(
      f: (A, B) => C): SessionEvent[C] = {
    val newUnder = underlying.snapshotWith(ev.underlying) {
      (cfA: Map[Client, A], cfB: Map[Client, B]) =>
        cfA.map2(cfB)(f)
    }
    new SessionEvent(newUnder, ev.graph.mergeGraphAndEffect(this.graph))
  }

  override def snapshotWith[B, C](ev: SessionDBehavior[B])(
      f: (A, B) => C): SessionDBehavior[C] = {
    val newUnder = underlying.snapshotWith(ev.underlying) {
      (cfA: Map[Client, A], cfB: Map[Client, B]) =>
        cfA.map2(cfB)(f)
    }
    new SessionDBehavior(newUnder,
                         this.initial.map(a => f(a, ev.initial)).force,
                         ev.graph.mergeGraphAndEffect(this.graph))
  }
}

object SessionBehavior extends BehaviorObject[SessionTier] {
  override def constant[A](x: A): SessionTier#Behavior[A] = {
    val clientMap = AppBehavior.clients.map { clients =>
      clients.map(_ -> x).toMap
    }
    val state = clientMap.graph.xhr
    new SessionBehavior(clientMap, state, Thunk.eager(x))
  }

  override def fromPoll[A](f: () => A): SessionBehavior[A] =
    AppBehavior.toSession(AppBehavior.fromPoll(f))

  def toApp[A](sb: SessionBehavior[A]): AppBehavior[Map[Client, A]] =
    sb.underlying

  val client: SessionBehavior[Client] = {
    val clientMap = AppBehavior.clients.map { clients =>
      clients.map(c => c -> c).toMap
    }
    val state = clientMap.graph.ws
    new SessionBehavior(clientMap, state, Thunk.eager(ClientGenerator.static))
  }
}
