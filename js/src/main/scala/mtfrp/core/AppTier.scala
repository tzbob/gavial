package mtfrp
package core

import hokko.core.{IBehavior, Event => HEvent}
import io.circe._
import mtfrp.core.impl.HokkoBuilder
import mtfrp.core.mock._

// Define all App types
// Create all App constructors

class AppEvent[A] private[core] (graph: ReplicationGraph)
    extends MockEvent[AppTier, A](graph)

object AppEvent extends MockEventObject with AppEventObject {
  private[core] def toClient[A](appEv: AppEvent[Client => Option[A]])(
      implicit da: Decoder[A],
      ea: Encoder[A]): ClientEvent[A] = {
    val hokkoBuilder = implicitly[HokkoBuilder[ClientTier]]
    val newGraph     = ReplicationGraphClient.ReceiverEvent(appEv.graph)
    hokkoBuilder.event(newGraph.source, newGraph)
  }

  val clientChanges: AppEvent[ClientChange] = new AppEvent(
    ReplicationGraph.start)
}

class AppBehavior[A] private[core] (graph: ReplicationGraph)
    extends MockBehavior[AppTier, A](graph)

object AppBehavior extends MockBehaviorObject[AppTier] with AppBehaviorObject

class AppDiscreteBehavior[A] private[core] (
    graph: ReplicationGraph,
    initial: A
) extends MockDiscreteBehavior[AppTier, A](graph, initial)

object AppDiscreteBehavior
    extends MockDiscreteBehaviorObject[AppTier]
    with AppDiscreteBehaviorObject

class AppIncBehavior[A, DeltaA] private[core] (
    graph: ReplicationGraph,
    accumulator: (A, DeltaA) => A,
    initial: A
) extends MockIncBehavior[AppTier, A, DeltaA](graph, accumulator, initial)

import slogging.LazyLogging

object AppIncBehavior
    extends MockIncrementalBehaviorObject[AppTier]
    with AppIncBehaviorObject
    with LazyLogging {

  def toClient[A, DeltaA](
      appBeh: AppIncBehavior[Client => A, Client => Option[DeltaA]])(
      implicit da: Decoder[A],
      dda: Decoder[DeltaA],
      ea: Encoder[A],
      eda: Encoder[DeltaA]): ClientIncBehavior[A, DeltaA] = {

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
    val init: A = appBeh.initial(ClientGenerator.static)

    val transformedAccumulator =
      IncrementalBehavior.transformToNormal(appBeh.accumulator)

    val replicatedBehavior: IBehavior[A, DeltaA] =
      deltas
        .fold(init) { (acc, n) =>
          transformedAccumulator(acc, n)
        }
        .resetState(resets)

    hokkoBuilder.incrementalBehavior(replicatedBehavior,
                                     init,
                                     newGraph,
                                     transformedAccumulator)
  }
}

final class AppTier extends MockTier with AppTierLike
