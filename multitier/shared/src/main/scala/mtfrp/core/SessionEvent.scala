package mtfrp.core

import hokko.{core => HC}
import io.circe._

class SessionEvent[A] private[core] (
    private[core] val underlying: AppEvent[Map[Client, A]],
    private[core] val graph: GraphState
) extends Event[SessionTier, A] {

  def collect[B](fb: A => Option[B]): SessionTier#Event[B] = {
    val newUnderlying = underlying.collect { clientMap: Map[Client, A] =>
      val mappedMap = clientMap.mapValues(fb)

      val filteredMap = mappedMap.filter(_._2.isDefined)
      if (filteredMap.isEmpty) None
      else Option(filteredMap.mapValues(_.get))
    }

    new SessionEvent(newUnderlying, this.graph)
  }

  def fold[B](initial: B)(f: (B, A) => B): SessionTier#IBehavior[B, A] = {
    val initialMap = Map.empty[Client, B]
    val newRep = underlying.fold(initialMap) {
      (stateMap: Map[Client, B], newEvent: Map[Client, A]) =>
        SessionIBehavior.accumulatorFunWithInitial(stateMap, newEvent, initial)(
          f)
    }

    new SessionIBehavior(newRep, initial, this.graph)
  }

  def unionWith(b: SessionTier#Event[A])(
      f: (A, A) => A): SessionTier#Event[A] = {
    val newRep = underlying.unionWith(b.underlying) {
      (leftMap: Map[Client, A], rightMap: Map[Client, A]) =>
        import cats.implicits._

        val commonMap = leftMap.map2(rightMap)(f)
        leftMap ++ rightMap ++ commonMap
    }

    val state = GraphState.any.combine(this.graph, b.graph)
    new SessionEvent(newRep, state)
  }
}

object SessionEvent extends EventObject[SessionTier] {
  override def empty[A]: SessionEvent[A] =
    new SessionEvent(AppEvent.empty[Map[Client, A]], GraphState.default)

  override private[core] def apply[A](
      ev: HC.Event[A],
      graphState: GraphState): SessionEvent[A] = {
    val appMap: AppEvent[Map[Client, A]] =
      AppBehavior.clients.snapshotWith(AppEvent(ev, graphState)) {
        (clients, ev) =>
          clients.map(_ -> ev).toMap
      }
    new SessionEvent(appMap, graphState)
  }

  def toApp[A](sessionEvent: SessionEvent[A]): AppEvent[Map[Client, A]] = {
    sessionEvent.underlying
  }

  def toClient[A](sessionEvent: SessionEvent[A])(
      implicit dec: Decoder[A],
      enc: Encoder[A]): ClientEvent[A] =
    AppEvent.toClient(sessionEvent.underlying.map(_.get))
}
