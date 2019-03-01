package mtfrp
package core

import cats.data.Ior
import hokko.core.{Engine, Thunk}
import io.circe.{Decoder, Encoder}
import mtfrp.core.impl.HokkoBuilder
import mtfrp.core.mock._
import slogging.LazyLogging

class ClientEvent[A] private[core] (graph: GraphState)
    extends MockEvent[ClientTier, A](graph)

class ClientEventSource[A] private[core] (
    graph: GraphState
) extends ClientEvent[A](graph)

object ClientEvent extends MockEventObject[ClientTier] with ClientEventObject {
  def source[A]: ClientEventSource[A] =
    new ClientEventSource(GraphState.default)

  private[core] def toAppWithClient[A: Decoder: Encoder](
      clientEv: ClientEvent[A]): AppEvent[(Client, A)] = {
    val hokkoBuilder = implicitly[HokkoBuilder[AppTier]]

    val src = hokko.core.Event.source[(Client, A)]
    val receiverGraph = clientEv.graph.replicationGraph.map { rg =>
      ReplicationGraphServer.ReceiverEvent(rg, src)
    }

    hokkoBuilder.event(src, clientEv.graph.withGraph(receiverGraph))
  }

  def sourceWithEngineEffect[A](
      eff: (A => Unit) => Unit): ClientEventSource[A] = {
    new ClientEventSource(GraphState.default)
  }
}

class ClientBehavior[A] private[core] (graph: GraphState, initial: Thunk[A])
    extends MockBehavior[ClientTier, A](graph, initial)

class ClientBehaviorSink[A] private[core] (graph: GraphState, initial: Thunk[A])
    extends ClientBehavior[A](graph, initial)

object ClientBehavior extends MockBehaviorObject[ClientTier] {
  def sink[A](default: A): ClientBehaviorSink[A] = new ClientBehaviorSink(
    GraphState.default,
    Thunk.eager(default)
  )
}

class ClientDBehavior[A] private[core] (
    graph: => GraphState,
    initial: => A
) extends MockDBehavior[ClientTier, A](graph, initial)

object ClientDBehavior
    extends MockDBehaviorObject[ClientTier]
    with ClientDBehaviorObject

class ClientIBehavior[A, DeltaA] private[core] (
    graph: GraphState,
    accumulator: (A, DeltaA) => A,
    initial: A
) extends MockIBehavior[ClientTier, A, DeltaA](graph, accumulator, initial)

object ClientIBehavior
    extends MockIBehaviorObject
    with ClientIBehaviorObject
    with LazyLogging {
  def toApp[A: Decoder: Encoder, DeltaA: Decoder: Encoder](
      clientBeh: ClientIBehavior[A, DeltaA])
    : AppIBehavior[Map[Client, A], Ior[(Client, DeltaA), ClientChange]] = {
    val hokkoBuilder = implicitly[HokkoBuilder[AppTier]]

    val deltas = hokko.core.Event.source[(Client, DeltaA)]
    val newGraph = clientBeh.graph.replicationGraph.map { rg =>
      ReplicationGraphServer.ReceiverBehavior[A, DeltaA](rg, deltas)
    }
    val state = clientBeh.graph.withGraph(newGraph)

    val clientChangesIor =
      AppEvent.clientChanges.rep.map(Ior.right[(Client, DeltaA), ClientChange])
    val deltasChanges: hokko.core.Event[Ior[(Client, DeltaA), ClientChange]] =
      deltas
        .map(Ior.left[(Client, DeltaA), ClientChange])
        .unionWith(clientChangesIor) {
          case (Ior.Left(pulse), Ior.Right(change)) => Ior.Both(pulse, change)
          case _ =>
            throw new RuntimeException("Impossible union crash")
        }

    val defaultValue = Map.empty[Client, A]
    val accumulator = IBehavior.transformFromNormalToClientChange(
      clientBeh.initial,
      clientBeh.accumulator)

    val behavior = deltasChanges.foldI(defaultValue)(accumulator)

    hokkoBuilder.IBehavior(behavior, state)
  }
}

object ClientAsync extends MockAsync[ClientTier]

class ClientTier extends MockTier with ClientTierLike
final object ClientTier extends ClientTier
