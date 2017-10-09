package mtfrp
package core

import io.circe.{Decoder, Encoder}
import mtfrp.core.impl.HokkoBuilder
import mtfrp.core.mock._

import scala.collection.immutable

// Define all Client types
// Create all Client constructors

class ClientEvent[A] private[core] (graph: ReplicationGraph)
    extends MockEvent[ClientTier, A](graph)

class ClientEventSource[A] private[core] (
    graph: ReplicationGraph
) extends ClientEvent[A](graph)

object ClientEvent extends MockEventObject[ClientTier] {
  def source[A]: ClientEventSource[A] =
    new ClientEventSource(ReplicationGraph.start)

  def toApp[A: Decoder: Encoder](
      clientEv: ClientEvent[A]): AppEvent[(Client, A)] = {
    val hokkoBuilder  = implicitly[HokkoBuilder[AppTier]]
    val receiverGraph = ReplicationGraphServer.ReceiverEvent(clientEv.graph)
    hokkoBuilder.event(receiverGraph.source, receiverGraph)
  }

  def toSession[A: Decoder: Encoder](
      clientEv: ClientEvent[A]): SessionEvent[A] =
    new SessionEvent(toApp(clientEv).map {
      case (c, a) =>
        (c0: Client) =>
          if (c == c0) Some(a) else None
    })
}

class ClientBehavior[A] private[core] (graph: ReplicationGraph)
    extends MockBehavior[ClientTier, A](graph)

class ClientBehaviorSink[A] private[core] (graph: ReplicationGraph)
    extends ClientBehavior[A](graph)

object ClientBehavior extends MockBehaviorObject[ClientTier] {
  def sink[A](default: A): ClientBehaviorSink[A] = new ClientBehaviorSink(
    ReplicationGraph.start
  )
}

class ClientDBehavior[A] private[core](
    graph: ReplicationGraph,
    initial: A
) extends MockDBehavior[ClientTier, A](graph, initial)

object ClientDBehavior extends MockDBehaviorObject[ClientTier]

class ClientIBehavior[A, DeltaA] private[core] (
    graph: ReplicationGraph,
    accumulator: (A, DeltaA) => A,
    initial: A
) extends MockIBehavior[ClientTier, A, DeltaA](graph, accumulator, initial)

object ClientIBehavior extends MockIBehaviorObject {
  def toApp[A: Decoder: Encoder, DeltaA: Decoder: Encoder](
      clientBeh: ClientIBehavior[A, DeltaA])
    : AppIBehavior[immutable.Map[Client, A], (Client, DeltaA)] = {
    val hokkoBuilder = implicitly[HokkoBuilder[AppTier]]

    val newGraph =
      ReplicationGraphServer.ReceiverBehavior[A, DeltaA](clientBeh.graph)

    val transformed =
      IBehavior.transformFromNormal(clientBeh.accumulator)
    // FIXME: relying on Map.default is dangerous
    val defaultValue =
      Map.empty[Client, A].withDefaultValue(clientBeh.initial)

    val deltas   = newGraph.deltas.source
    val behavior = deltas.fold(defaultValue)(transformed)

    hokkoBuilder
      .IBehavior(behavior, defaultValue, newGraph, transformed)
  }

  def toSession[A: Decoder: Encoder, DeltaA: Decoder: Encoder](
      clientBeh: ClientIBehavior[A, DeltaA])
    : SessionIBehavior[A, DeltaA] = {
    val ib: AppIBehavior[Client => A, Client => Option[DeltaA]] =
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

    new SessionIBehavior(ib)
  }
}

final class ClientTier extends MockTier with ClientTierLike
