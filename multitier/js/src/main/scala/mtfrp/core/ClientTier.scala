package mtfrp
package core

import hokko.core
import hokko.core.Engine
import io.circe.{Decoder, Encoder}
import mtfrp.core.impl._
import mtfrp.core.mock.MockBuilder

class ClientEvent[A] private[core] (rep: core.Event[A], graph: GraphState)
    extends HokkoEvent[ClientTier, A](rep, graph)

class ClientEventSource[A] private[core] (override val rep: core.EventSource[A],
                                          graph: GraphState)
    extends ClientEvent[A](rep, graph)

object ClientEvent extends HokkoEventObject with ClientEventObject {
  private[core] def toAppWithClient[A: Decoder: Encoder](
      clientEv: ClientEvent[A]): AppEvent[(Client, A)] = {
    val mockBuilder = implicitly[MockBuilder[AppTier]]
    val newGraph = ReplicationGraphClient.SenderEvent(
      clientEv.rep,
      clientEv.graph.replicationGraph)
    val graph = clientEv.graph.withGraph(newGraph)
    mockBuilder.event(graph)
  }

  def source[A]: ClientEventSource[A] =
    new ClientEventSource(core.Event.source[A], GraphState.default)

  def sourceWithEngineEffect[A](
      eff: (Engine, (A => Unit)) => Unit): ClientEventSource[A] = {
    val src = core.Event.source[A]
    new ClientEventSource(src, GraphState.default.withEffect { (e: Engine) =>
      eff(e, a => e.fire(Seq(src -> a)))
    })
  }
}

class ClientBehavior[A] private[core] (
    rep: core.CBehavior[A],
    graph: GraphState
) extends HokkoBehavior[ClientTier, A](rep, graph)

class ClientBehaviorSink[A] private[core] (
    override val rep: core.CBehaviorSource[A],
    graph: GraphState
) extends ClientBehavior[A](rep, graph)

object ClientBehavior extends HokkoBehaviorObject[ClientTier] {
  def sink[A](default: A): ClientBehaviorSink[A] =
    new ClientBehaviorSink(core.CBehavior.source(default), GraphState.default)
}

class ClientDBehavior[A] private[core] (
    rep: core.DBehavior[A],
    graph: => GraphState
) extends HokkoDBehavior[ClientTier, A](rep, graph)

object ClientDBehavior extends HokkoDBehaviorObject[ClientTier]

class ClientIBehavior[A, DeltaA] private[core] (
    rep: core.IBehavior[A, DeltaA],
    graph: GraphState
) extends HokkoIBehavior[ClientTier, A, DeltaA](rep, graph)

object ClientIBehavior extends HokkoIBehaviorObject with ClientIBehaviorObject {
  def toApp[A: Decoder: Encoder, DeltaA: Decoder: Encoder](
      clientBeh: ClientIBehavior[A, DeltaA])
    : AppIBehavior[Map[Client, A], (Client, DeltaA)] = {
    val mockBuilder = implicitly[MockBuilder[AppTier]]

    val newGraph = ReplicationGraphClient.SenderBehavior(
      clientBeh.rep,
      clientBeh.graph.replicationGraph)

    val transformed = IBehavior.transformFromNormal(clientBeh.accumulator)
    // FIXME: relying on Map.default is dangerous
    val defaultValue =
      Map.empty[Client, A].withDefaultValue(clientBeh.initial)

    mockBuilder.IBehavior(clientBeh.graph.withGraph(newGraph),
                          transformed,
                          defaultValue)
  }
}

object ClientAsync extends HokkoAsync[ClientTier]

final class ClientTier extends HokkoTier with ClientTierLike
