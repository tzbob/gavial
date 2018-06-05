package mtfrp
package core

import hokko.core
import io.circe._
import mtfrp.core.impl.{HokkoAsync, HokkoBuilder}
import mtfrp.core.mock._
import slogging.LazyLogging

// Define all App types
// Create all App constructors

class AppEvent[A] private[core] (graph: ReplicationGraph,
                                 requiresWebSockets: Boolean)
    extends MockEvent[AppTier, A](graph, requiresWebSockets)

object AppEvent extends MockEventObject with AppEventObject {
  private[core] def toClient[A](appEv: AppEvent[Client => Option[A]])(
      implicit da: Decoder[A],
      ea: Encoder[A]): ClientEvent[A] = {
    val hokkoBuilder = implicitly[HokkoBuilder[ClientTier]]
    val newGraph     = ReplicationGraphClient.ReceiverEvent(appEv.graph)
    hokkoBuilder.event(newGraph.source, newGraph, true)
  }

  val start: AppEvent[ClientChange] =
    new AppEvent(ReplicationGraph.start, false)
  val clientChanges: AppEvent[ClientChange] =
    new AppEvent(ReplicationGraph.start, true)
}

class AppBehavior[A] private[core] (graph: ReplicationGraph,
                                    requiresWebSockets: Boolean)
    extends MockBehavior[AppTier, A](graph, requiresWebSockets)

object AppBehavior extends MockBehaviorObject[AppTier] with AppBehaviorObject

class AppDBehavior[A] private[core] (
    graph: ReplicationGraph,
    initial: A,
    requiresWebSockets: Boolean
) extends MockDBehavior[AppTier, A](graph, initial, requiresWebSockets)

object AppDBehavior extends MockDBehaviorObject[AppTier] with AppDBehaviorObject

class AppIBehavior[A, DeltaA] private[core] (
    graph: ReplicationGraph,
    accumulator: (A, DeltaA) => A,
    initial: A,
    requiresWebSockets: Boolean
) extends MockIBehavior[AppTier, A, DeltaA](graph,
                                              accumulator,
                                              initial,
                                              requiresWebSockets)

object AppIBehavior
    extends MockIBehaviorObject[AppTier]
    with AppIBehaviorObject
    with LazyLogging {

  def toClient[A, DeltaA](
      appBeh: AppIBehavior[Client => A, Client => Option[DeltaA]],
      init: A)(implicit da: Decoder[A],
               dda: Decoder[DeltaA],
               ea: Encoder[A],
               eda: Encoder[DeltaA]): ClientIBehavior[A, DeltaA] = {

    val hokkoBuilder = implicitly[HokkoBuilder[ClientTier]]

    val newGraph =
      ReplicationGraphClient.ReceiverBehavior[A, DeltaA](appBeh.graph)

    val deltas = newGraph.deltas.source
    val resets = newGraph.resets

    // TODO: Improve with an initial value reader/injector
    /*
    This is correct, the actual initial values are sent as a
    reset request, the initial values of behaviors are only used as
    an asap-initialisation mechanism.
     */

    val transformedAccumulator =
      IBehavior.transformToNormal(appBeh.accumulator)

    val replicatedBehavior: core.IBehavior[A, DeltaA] =
      deltas
        .resetFold(resets)(init) { (acc, n) =>
          transformedAccumulator(acc, n)
        }

    hokkoBuilder.IBehavior(replicatedBehavior,
                           init,
                           newGraph,
                           transformedAccumulator,
                           true)
  }
}

object AppAsync extends MockAsync[AppTier]

final class AppTier extends MockTier with AppTierLike
