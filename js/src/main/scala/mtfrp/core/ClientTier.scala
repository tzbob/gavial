package mtfrp
package core

import hokko.core
import io.circe.{Decoder, Encoder}
import mtfrp.core.impl._
import mtfrp.core.mock.MockBuilder

import scala.collection.immutable

// Define all Client types
// Create all Client constructors

class ClientEvent[A] private[core] (rep: core.Event[A], graph: ReplicationGraph)
    extends HokkoEvent[ClientTier, A](rep, graph)

class ClientEventSource[A] private[core] (
    override val rep: core.EventSource[A],
    graph: ReplicationGraph
) extends ClientEvent[A](rep, graph)

object ClientEvent extends HokkoEventObject {
  def source[A]: ClientEventSource[A] =
    new ClientEventSource(core.Event.source[A], ReplicationGraph.start)

  private[core] def toApp[A: Decoder: Encoder](
      clientEv: ClientEvent[A]): AppEvent[(Client, A)] = {
    val mockBuilder = implicitly[MockBuilder[AppTier]]
    val newGraph =
      ReplicationGraphClient.SenderEvent(clientEv.rep, clientEv.graph)
    mockBuilder.event(newGraph)
  }

  def toSession[A: Decoder: Encoder](
      clientEv: ClientEvent[A]): SessionEvent[A] =
    new SessionEvent(toApp(clientEv).map {
      case (c, a) =>
        (c0: Client) =>
          if (c == c0) Some(a) else None
    })
}

class ClientBehavior[A] private[core] (
    rep: core.CBehavior[A],
    graph: ReplicationGraph
) extends HokkoBehavior[ClientTier, A](rep, graph)

class ClientBehaviorSink[A] private[core] (
    override val rep: core.CBehaviorSource[A],
    graph: ReplicationGraph
) extends ClientBehavior[A](rep, graph)

object ClientBehavior extends HokkoBehaviorObject[ClientTier] {
  def sink[A](default: A): ClientBehaviorSink[A] = new ClientBehaviorSink(
    core.CBehavior.source(default),
    ReplicationGraph.start
  )
}

class ClientDiscreteBehavior[A] private[core] (
    rep: core.DBehavior[A],
    initial: A,
    graph: ReplicationGraph
) extends HokkoDiscreteBehavior[ClientTier, A](rep, initial, graph)

object ClientDiscreteBehavior extends HokkoDiscreteBehaviorObject[ClientTier]

class ClientIncBehavior[A, DeltaA] private[core] (
    rep: core.IBehavior[A, DeltaA],
    initial: A,
    graph: ReplicationGraph,
    accumulator: (A, DeltaA) => A
) extends HokkoIncBehavior[ClientTier, A, DeltaA](rep,
                                                    initial,
                                                    graph,
                                                    accumulator)

object ClientIncBehavior extends HokkoIncrementalBehaviorObject {
  def toApp[A: Decoder: Encoder, DeltaA: Decoder: Encoder](
      clientBeh: ClientIncBehavior[A, DeltaA])
    : AppIncBehavior[immutable.Map[Client, A], (Client, DeltaA)] = {
    val mockBuilder = implicitly[MockBuilder[AppTier]]

    val newGraph =
      ReplicationGraphClient.SenderBehavior(clientBeh.rep, clientBeh.graph)

    val transformed =
      IncrementalBehavior.transformFromNormal(clientBeh.accumulator)
    // FIXME: relying on Map.default is dangerous
    val defaultValue =
      Map.empty[Client, A].withDefaultValue(clientBeh.initial)

    mockBuilder.incrementalBehavior(newGraph, transformed, defaultValue)
  }

  def toSession[A: Decoder: Encoder, DeltaA: Decoder: Encoder](
      clientBeh: ClientIncBehavior[A, DeltaA])
    : SessionIncBehavior[A, DeltaA] = {
    val ib: AppIncBehavior[Client => A, Client => Option[DeltaA]] =
      toApp(clientBeh).map { m =>
        m.apply _
      } {
        case (c, deltaA) =>
          (c0: Client) =>
            if (c0 == c) Some(deltaA) else None
      } { (cfA, cfOptDA) => (c: Client) =>
        val a = cfA(c)
        cfOptDA(c) match {
          case Some(da) => clientBeh.accumulator(a, da)
          case None     => a
        }
      }

    new SessionIncBehavior(ib)
  }
}

final class ClientTier extends HokkoTier with ClientTierLike
