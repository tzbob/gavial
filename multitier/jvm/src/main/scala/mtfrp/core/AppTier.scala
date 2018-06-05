package mtfrp
package core

import hokko.core
import io.circe._
import mtfrp.core.impl._
import mtfrp.core.mock.MockBuilder

// Define all App types
// Create all App constructors

class AppEvent[A] private[core] (
    rep: core.Event[A],
    graph: ReplicationGraph,
    requiresWebSockets: Boolean
) extends HokkoEvent[AppTier, A](rep, graph, requiresWebSockets)

object AppEvent extends HokkoEventObject with AppEventObject {
  private[core] def toClient[A](appEv: AppEvent[Client => Option[A]])(
      implicit da: Decoder[A],
      ea: Encoder[A]): ClientEvent[A] = {
    val mockBuilder = implicitly[MockBuilder[ClientTier]]
    val newGraph    = ReplicationGraphServer.SenderEvent(appEv.rep, appEv.graph)
    mockBuilder.event(newGraph, true)
  }

  private[core] val serverStart = core.Event.source[Unit]
  val start: AppEvent[Unit] =
    new AppEvent(serverStart, ReplicationGraph.start, false)
  private[core] val clientChangesSource = core.Event.source[ClientChange]
  val clientChanges: AppEvent[ClientChange] =
    new AppEvent(clientChangesSource, ReplicationGraph.start, true)
}

class AppBehavior[A] private[core] (
    rep: core.CBehavior[A],
    graph: ReplicationGraph,
    requiresWebSockets: Boolean
) extends HokkoBehavior[AppTier, A](rep, graph, requiresWebSockets)

object AppBehavior extends HokkoBehaviorObject[AppTier] with AppBehaviorObject

class AppDBehavior[A] private[core] (
    rep: core.DBehavior[A],
    initial: A,
    graph: ReplicationGraph,
    requiresWebSockets: Boolean
) extends HokkoDBehavior[AppTier, A](rep, initial, graph, requiresWebSockets)

object AppDBehavior
    extends HokkoDBehaviorObject[AppTier]
    with AppDBehaviorObject

class AppIBehavior[A, DeltaA] private[core] (
    rep: core.IBehavior[A, DeltaA],
    initial: A,
    graph: ReplicationGraph,
    accumulator: (A, DeltaA) => A,
    requiresWebSockets: Boolean
) extends HokkoIBehavior[AppTier, A, DeltaA](rep,
                                               initial,
                                               graph,
                                               accumulator,
                                               requiresWebSockets)

object AppIBehavior
    extends HokkoIBehaviorObject[AppTier]
    with AppIBehaviorObject {

  def toClient[A, DeltaA](
      appBeh: AppIBehavior[Client => A, Client => Option[DeltaA]],
      init: A)(implicit da: Decoder[A],
               dda: Decoder[DeltaA],
               ea: Encoder[A],
               eda: Encoder[DeltaA]): ClientIBehavior[A, DeltaA] = {

    val accumulator = IBehavior.transformToNormal(appBeh.accumulator)

    Replicator.toClient(init,
                        accumulator,
                        appBeh.toDBehavior.toBehavior,
                        appBeh.deltas)
  }

}

object AppAsync extends HokkoAsync[AppTier]

final class AppTier extends HokkoTier with AppTierLike
