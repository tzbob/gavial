package mtfrp
package core

import hokko.core
import hokko.core.tc
import io.circe._

// Define all App types
// Create all App constructors

class AppEvent[A] private[core] (
    rep: core.Event[A],
    graph: ReplicationGraph
) extends HokkoEvent[AppTier, A](rep, graph)

object AppEvent extends HokkoEventObject {
  implicit val mtfrpAppEventInstances: tc.Event[AppEvent, AppIncBehavior] =
    this.makeInstances[AppTier]

  implicit class ToClientEvent[A](appEv: AppEvent[Client => Option[A]]) {
    def toClient(implicit da: Decoder[A], ea: Encoder[A]): ClientEvent[A] = {
      val mockBuilder = implicitly[MockBuilder[ClientTier]]
      val newGraph    = ReplicationGraphServer.SenderEvent(appEv.rep, appEv.graph)
      mockBuilder.event(newGraph)
    }
  }
}

class AppBehavior[A] private[core] (
    rep: core.CBehavior[A],
    graph: ReplicationGraph
) extends HokkoBehavior[AppTier, A](rep, graph)

object AppBehavior extends HokkoBehaviorObject[AppTier] {
  implicit val mtfrpBehaviorInstances = this.makeInstances
}

class AppDiscreteBehavior[A] private[core] (
    rep: core.DBehavior[A],
    initial: A,
    graph: ReplicationGraph
) extends HokkoDiscreteBehavior[AppTier, A](rep, initial, graph)

object AppDiscreteBehavior extends HokkoDiscreteBehaviorObject[AppTier] {
  implicit val mtfrpDBehaviorInstances = this.makeInstances
}

class AppIncBehavior[A, DeltaA] private[core] (
    rep: core.IBehavior[A, DeltaA],
    initial: A,
    graph: ReplicationGraph,
    accumulator: (A, DeltaA) => A
) extends HokkoIncBehavior[AppTier, A, DeltaA](rep,
                                                 initial,
                                                 graph,
                                                 accumulator)

object AppIncBehavior extends HokkoIncrementalBehaviorObject[AppTier] {
  implicit val mtfrpDBehaviorInstances = this.makeInstances

  implicit class ToClientBehavior[A, DeltaA](
      appBeh: AppIncBehavior[Client => A, Client => Option[DeltaA]]) {
    def toClient(implicit da: Decoder[A],
                 dda: Decoder[DeltaA],
                 ea: Encoder[A],
                 eda: Encoder[DeltaA]): ClientIncBehavior[A, DeltaA] = {
      val mockBuilder = implicitly[MockBuilder[ClientTier]]
      val newGraph =
        ReplicationGraphServer.SenderBehavior(appBeh.rep, appBeh.graph)

      val accumulator =
        IncrementalBehavior.transformToNormal(appBeh.accumulator)
      // Create the initial value by evaluating the function with a fresh client
      val initialFromFreshClient = appBeh.initial(ClientGenerator.fresh)

      mockBuilder
        .incrementalBehavior(newGraph, accumulator, initialFromFreshClient)
    }
  }
}

final class AppTier extends HokkoTier with AppTierLike
