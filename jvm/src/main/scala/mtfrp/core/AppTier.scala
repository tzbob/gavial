package mtfrp
package core

import hokko.core
import io.circe._

// Define all App types
// Create all App constructors

class AppEvent[A] private[core] (
    rep: core.Event[A],
    graph: ReplicationGraph
) extends HokkoEvent[AppTier, A](rep, graph)

object AppEvent {
  def apply[A](ev: core.Event[A]): AppEvent[A] =
    new AppEvent(ev, ReplicationGraph.start)

  def empty[A]: AppEvent[A] =
    new AppEvent(core.Event.source[A], ReplicationGraph.start)

  implicit class ToClientEvent[A](appEv: AppEvent[Client => Option[A]]) {
    def toClient(implicit da: Decoder[A], ea: Encoder[A]): ClientEvent[A] = {
      val mockBuilder = implicitly[MockBuilder[ClientTier]]
      val newGraph    = ReplicationGraphServer.SenderEvent(appEv.rep, appEv.graph)
      mockBuilder.event(newGraph)
    }
  }
}

class AppBehavior[A] private[core] (
    rep: core.Behavior[A],
    graph: ReplicationGraph
) extends HokkoBehavior[AppTier, A](rep, graph)

class AppDiscreteBehavior[A] private[core] (
    rep: core.DiscreteBehavior[A],
    initial: A,
    graph: ReplicationGraph
) extends HokkoDiscreteBehavior[AppTier, A](rep, initial, graph)

class AppIncBehavior[A, DeltaA] private[core] (
    rep: core.IncrementalBehavior[A, DeltaA],
    initial: A,
    graph: ReplicationGraph,
    accumulator: (A, DeltaA) => A
) extends HokkoIncBehavior[AppTier, A, DeltaA](rep,
                                                 initial,
                                                 graph,
                                                 accumulator)

object AppIncBehavior {
  implicit class ToClientBehavior[A, DeltaA](
      appBeh: AppIncBehavior[Client => A, Client => Option[DeltaA]]) {
    def toClient(implicit da: Decoder[A],
                 dda: Decoder[DeltaA],
                 ea: Encoder[A],
                 eda: Encoder[DeltaA]): ClientIncBehavior[A, DeltaA] = {
      val mockBuilder = implicitly[MockBuilder[ClientTier]]
      val newGraph    = ReplicationGraphServer.SenderBehavior(appBeh.rep, appBeh.graph)

      val accumulator = IncrementalBehavior.transformToNormal(appBeh.accumulator)
      // Create the initial value by evaluating the function with a fresh client
      val initialFromFreshClient = appBeh.initial(ClientGenerator.fresh)

      mockBuilder
        .incrementalBehavior(newGraph, accumulator, initialFromFreshClient)
    }
  }
}

final class AppTier extends HokkoTier with AppTierLike {
  type T = AppTier
}

object AppBehavior extends HokkoBehaviorOps[AppTier]
object AppDiscreteBehavior extends HokkoDiscreteBehaviorOps[AppTier]
