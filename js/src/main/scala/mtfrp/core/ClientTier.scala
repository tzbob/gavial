package mtfrp
package core

import hokko.core
import io.circe.{Decoder, Encoder}
import mtfrp.core.impl._
import mtfrp.core.mock.MockBuilder

class ClientEvent[A] private[core] (rep: core.Event[A],
                                    graph: ReplicationGraph,
                                    requiresWebSockets: Boolean)
    extends HokkoEvent[ClientTier, A](rep, graph, requiresWebSockets)

class ClientEventSource[A] private[core] (override val rep: core.EventSource[A],
                                          graph: ReplicationGraph,
                                          requiresWebSockets: Boolean)
    extends ClientEvent[A](rep, graph, requiresWebSockets)

object ClientEvent extends HokkoEventObject with ClientEventObject {
  def source[A]: ClientEventSource[A] =
    new ClientEventSource(core.Event.source[A], ReplicationGraph.start, false)

  private[core] def toAppWithClient[A: Decoder: Encoder](
      clientEv: ClientEvent[A]): AppEvent[(Client, A)] = {
    val mockBuilder = implicitly[MockBuilder[AppTier]]
    val newGraph =
      ReplicationGraphClient.SenderEvent(clientEv.rep, clientEv.graph)
    mockBuilder.event(newGraph, false)
  }
}

class ClientBehavior[A] private[core] (
    rep: core.CBehavior[A],
    graph: ReplicationGraph,
    requiresWebSockets: Boolean
) extends HokkoBehavior[ClientTier, A](rep, graph, requiresWebSockets)

class ClientBehaviorSink[A] private[core] (
    override val rep: core.CBehaviorSource[A],
    graph: ReplicationGraph,
    requiresWebSockets: Boolean
) extends ClientBehavior[A](rep, graph, requiresWebSockets)

object ClientBehavior extends HokkoBehaviorObject[ClientTier] {
  def sink[A](default: A): ClientBehaviorSink[A] =
    new ClientBehaviorSink(core.CBehavior.source(default),
                           ReplicationGraph.start,
                           false)
}

class ClientDBehavior[A] private[core] (
    rep: core.DBehavior[A],
    initial: A,
    graph: ReplicationGraph,
    requiresWebSockets: Boolean
) extends HokkoDBehavior[ClientTier, A](rep,
                                          initial,
                                          graph,
                                          requiresWebSockets)

object ClientDBehavior extends HokkoDBehaviorObject[ClientTier]

class ClientIBehavior[A, DeltaA] private[core] (
    rep: core.IBehavior[A, DeltaA],
    initial: A,
    graph: ReplicationGraph,
    accumulator: (A, DeltaA) => A,
    requiresWebSockets: Boolean
) extends HokkoIBehavior[ClientTier, A, DeltaA](rep,
                                                  initial,
                                                  graph,
                                                  accumulator,
                                                  requiresWebSockets)

object ClientIBehavior extends HokkoIBehaviorObject with ClientIBehaviorObject {
  def toApp[A: Decoder: Encoder, DeltaA: Decoder: Encoder](
      clientBeh: ClientIBehavior[A, DeltaA])
    : AppIBehavior[Map[Client, A], (Client, DeltaA)] = {
    val mockBuilder = implicitly[MockBuilder[AppTier]]

    val newGraph =
      ReplicationGraphClient.SenderBehavior(clientBeh.rep, clientBeh.graph)

    val transformed = IBehavior.transformFromNormal(clientBeh.accumulator)
    // FIXME: relying on Map.default is dangerous
    val defaultValue =
      Map.empty[Client, A].withDefaultValue(clientBeh.initial)

    mockBuilder.IBehavior(newGraph, transformed, defaultValue, false)
  }
}

object ClientAsync extends HokkoAsync[ClientTier]

final class ClientTier extends HokkoTier with ClientTierLike
