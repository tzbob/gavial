package mtfrp
package core

import hokko.core
import hokko.core.Engine
import io.circe._
import mtfrp.core.impl.{HokkoAsync, HokkoBuilder}
import mtfrp.core.mock._
import slogging.LazyLogging

// Define all App types
// Create all App constructors

class AppEvent[A] private[core] (graph: GraphState)
    extends MockEvent[AppTier, A](graph)

class AppEventSource[A] private[core] (graph: GraphState)
    extends AppEvent[A](graph)

object AppEvent extends MockEventObject with AppEventObject {
  private[core] def toClient[A](appEv: AppEvent[Client => Option[A]])(
      implicit da: Decoder[A],
      ea: Encoder[A]): ClientEvent[A] = {
    val hokkoBuilder = implicitly[HokkoBuilder[ClientTier]]
    val newGraph =
      ReplicationGraphClient.ReceiverEvent(appEv.graph.replicationGraph)
    val state = GraphState(true, newGraph, _ => ())
    hokkoBuilder.event(newGraph.source, state)
  }

  val start: AppEvent[ClientChange] =
    new AppEvent(GraphState.default)
  val clientChanges: AppEvent[ClientChange] =
    new AppEvent(GraphState.default.ws)

  def source[A]: AppEventSource[A] =
    new AppEventSource(GraphState.default)

  def sourceWithEngineEffect[A](
      eff: (Engine, (A => Unit)) => Unit): AppEventSource[A] = {
    new AppEventSource(GraphState.default)
  }
}

class AppBehavior[A] private[core] (graph: GraphState)
    extends MockBehavior[AppTier, A](graph)

object AppBehavior extends MockBehaviorObject[AppTier] with AppBehaviorObject

class AppDBehavior[A] private[core] (
    graph: => GraphState,
    initial: A
) extends MockDBehavior[AppTier, A](graph, initial)

object AppDBehavior extends MockDBehaviorObject[AppTier] with AppDBehaviorObject

class AppIBehavior[A, DeltaA] private[core] (
    graph: GraphState,
    accumulator: (A, DeltaA) => A,
    initial: A,
) extends MockIBehavior[AppTier, A, DeltaA](graph, accumulator, initial)

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
      ReplicationGraphClient.ReceiverBehavior[A, DeltaA](
        appBeh.graph.replicationGraph)

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
                           GraphState(true, newGraph, _ => ()),
                           transformedAccumulator)
  }
}

object AppAsync extends MockAsync[AppTier]

final class AppTier extends MockTier with AppTierLike
