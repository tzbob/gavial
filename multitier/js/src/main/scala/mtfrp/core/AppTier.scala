package mtfrp
package core

import cats.Eval
import hokko.core
import hokko.core.{Engine, Thunk}
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
    val src          = hokko.core.Event.source[A]
    val newGraph = appEv.graph.replicationGraph.map { rg =>
      ReplicationGraphClient.ReceiverEvent(rg, src)
    }
    val state = appEv.graph.ws.withGraph(newGraph)
    hokkoBuilder.event(src, state)
  }

  val start: AppEvent[ClientChange] =
    new AppEvent(GraphState.default)
  val clientChanges: AppEvent[ClientChange] =
    new AppEvent(GraphState.default.ws)

  def source[A]: AppEventSource[A] =
    new AppEventSource(GraphState.default)

  def sourceWithEngineEffect[A](eff: (A => Unit) => Unit): AppEventSource[A] = {
    new AppEventSource(GraphState.default)
  }
}

class AppBehavior[A] private[core] (graph: GraphState, initial: Thunk[A])
    extends MockBehavior[AppTier, A](graph, initial)

object AppBehavior extends MockBehaviorObject[AppTier] with AppBehaviorObject

class AppDBehavior[A] private[core] (
    graph: => GraphState,
    initial: => A
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

    val transformedAccumulator =
      IBehavior.transformToNormal(appBeh.accumulator)

    Replicator.toClient(
      init,
      transformedAccumulator,
      appBeh.toDBehavior.toBehavior,
      appBeh.deltas
    )
  }
}

object AppAsync extends MockAsync[AppTier]

class AppTier extends MockTier with AppTierLike
final object AppTier extends AppTier
