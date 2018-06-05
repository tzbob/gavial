package mtfrp.core

import hokko.{core => HC}
import io.circe._

class SessionEvent[A] private[core] (
    private[core] val underlying: AppEvent[Map[Client, A]],
    private[core] val requiresWebSockets: Boolean
) extends Event[SessionTier, A] {

  private[core] val graph: ReplicationGraph = underlying.graph

  def collect[B](fb: A => Option[B]): SessionTier#Event[B] = {
    val newUnderlying = underlying.collect { clientMap: Map[Client, A] =>
      val mappedMap = clientMap.mapValues(fb)

      val filteredMap = mappedMap.filter(_._2.isDefined)
      if (filteredMap.isEmpty) None
      else Option(filteredMap.mapValues(_.get))
    }
    new SessionEvent(newUnderlying, this.requiresWebSockets)
  }

  def fold[B](initial: B)(f: (B, A) => B): SessionTier#IBehavior[B, A] = {
    val initialMap = Map.empty[Client, B]
    val newRep = underlying.fold(initialMap) {
      (stateMap: Map[Client, B], newEvent: Map[Client, A]) =>
        SessionIBehavior.accumulatorFunWithInitial(stateMap, newEvent, initial)(
          f)
    }

    new SessionIBehavior(newRep, initial, this.requiresWebSockets)
  }

  def unionWith(b: SessionTier#Event[A])(
      f: (A, A) => A): SessionTier#Event[A] = {
    val newRep = underlying.unionWith(b.underlying) {
      (leftMap: Map[Client, A], rightMap: Map[Client, A]) =>
        import cats.implicits._

        val commonMap = leftMap.map2(rightMap)(f)
        leftMap ++ rightMap ++ commonMap
    }

    new SessionEvent(newRep, this.requiresWebSockets || b.requiresWebSockets)
  }
}

object SessionEvent extends EventObject[SessionTier] {
  override def empty[A]: SessionEvent[A] =
    new SessionEvent(AppEvent.empty[Map[Client, A]], false)
  override private[core] def apply[A](
      ev: HC.Event[A],
      requiresWebSockets: Boolean): SessionEvent[A] = {
    val appMap: AppEvent[Map[Client, A]] =
      AppBehavior.clients.snapshotWith(AppEvent(ev, requiresWebSockets)) {
        (clients, ev) =>
          clients.map(_ -> ev).toMap
      }
    new SessionEvent(appMap, requiresWebSockets)
  }

  def toApp[A](sessionEvent: SessionEvent[A]): AppEvent[Map[Client, A]] = {
    sessionEvent.underlying
  }

  def toClient[A](sessionEvent: SessionEvent[A])(
      implicit dec: Decoder[A],
      enc: Encoder[A]): ClientEvent[A] =
    AppEvent.toClient(sessionEvent.underlying.map(_.get))
}
