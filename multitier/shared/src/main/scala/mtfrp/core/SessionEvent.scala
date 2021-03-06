package mtfrp.core

import cats.data.Ior
import hokko.{core => HC}
import io.circe._

class SessionEvent[A] private[core] (
    private[core] val underlying: AppEvent[Map[Client, A]],
    graphByName: GraphState
) extends Event[SessionTier, A] {
  private[core] lazy val graph = graphByName

  def collect[B](fb: A => Option[B]): SessionTier#Event[B] = {
    val newUnderlying = underlying.collect { clientMap: Map[Client, A] =>
      val mappedMap = clientMap.mapValues(fb)

      val filteredMap = mappedMap.filter(_._2.isDefined)
      if (filteredMap.isEmpty) None
      else Option(filteredMap.mapValues(_.get))
    }

    new SessionEvent(newUnderlying, this.graph)
  }

  def foldI[B](initial: B)(f: (B, A) => B): SessionTier#IBehavior[B, A] = {
    val initialMap = Map.empty[Client, B]

    val changes = underlying.map { change =>
      change -> Option.empty[SessionChange[B]]
    }

    val clientChanges = AppEvent.clientChanges.map { change =>
      Map.empty[Client, A] -> Option(
        SessionChange.fromClientChange(change, initial))
    }

    val allChanges = changes.unionWith(clientChanges) { (l, r) =>
      (l._1, r._2)
    }

    val newRep = allChanges.foldI(initialMap)(
      IBehavior.transformFromNormalToSetClientChangeMapWithCurrent(f)
    )

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
