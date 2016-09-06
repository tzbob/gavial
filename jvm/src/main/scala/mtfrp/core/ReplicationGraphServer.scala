package mtfrp.core

import hokko.{core => HC}
import io.circe._
import mtfrp.core.ReplicationGraph.Pulse
import mtfrp.core.ReplicationGraphServer.ReceiverEvent

class ReplicationGraphServer(graph: ReplicationGraph) {
  val graphList = ReplicationGraph.toList(graph)

  val exitEvent: HC.Event[Client => Seq[Message]] = {
    // all senders that should be added to the exit event (events and deltas)
    val senders = graphList.collect {
      case s: ReplicationGraphServer.SenderEvent[_] =>
        s.message
      case s: ReplicationGraphServer.SenderBehavior[_, _] =>
        s.deltaSender.message
    }

    val mergedSenders = HC.Event.merge(senders)

    val mergedSendersOneClient = mergedSenders.map { evfs => (c: Client) =>
      // make a client function that finds all messages
      evfs.map { evf =>
        evf(c)
      }.flatten
    }
    mergedSendersOneClient
  }

  val exitBehavior: HC.Behavior[Client => Seq[Message]] = {
    // all senders that should be added to the exit behavior
    val senders = graphList.collect {
      case s: ReplicationGraphServer.SenderBehavior[_, _] => s.message
    }

    val mergedSenders =
      senders.foldLeft(HC.Behavior.constant(List.empty[Client => Message])) {
        (accB, newB) =>
          accB.map2(newB)(_ :+ _)
      }

    val mergedSendersOneClient = mergedSenders.map { evfs => (c: Client) =>
      evfs.map { evf =>
        evf(c)
      }
    }
    mergedSendersOneClient
  }

  val exitData: ExitData = ExitData(exitEvent, exitBehavior)

  val inputEventRouter: (Client, Message) => Option[Pulse] = {
    val pulseMakers: Map[Int, (Client, Message) => Option[Pulse]] =
      graphList.collect {
        case r: ReceiverEvent[_] => (r.token, r.pulse _)
      }.toMap
    (c: Client, m: Message) =>
      pulseMakers.get(m.id).flatMap(_ apply (c, m))
  }
}

object ReplicationGraphServer {

  case class SenderEvent[A: Encoder](
      event: HC.Event[Client => Option[A]],
      dependency: ReplicationGraph
  ) extends ReplicationGraph.HasDependency {
    val message = event.map { evf => c: Client =>
      evf(c).map(Message.fromPayload(this.token))
    }
  }

  case class SenderBehavior[A: Encoder, DeltaA: Encoder](
      behavior: HC.IncrementalBehavior[Client => A, Client => Option[DeltaA]],
      dependency: ReplicationGraph
  ) extends ReplicationGraph.HasDependency {
    val deltaSender = SenderEvent(behavior.deltas, dependency)
    val message = behavior.map { evf => c: Client =>
      Message.fromPayload(this.token)(evf(c))
    }
  }

  case class ReceiverEvent[A: Decoder](dependency: ReplicationGraph)
      extends ReplicationGraph.HasDependency {
    val source: HC.EventSource[(Client, A)] = HC.Event.source

    def pulse(c: Client, msg: Message): Option[Pulse] = {
      val decoded = msg.payload.as[A]
      decoded.toOption.map { a =>
        source -> (c -> a)
      }
    }
  }

  case class ReceiverBehavior[A, DeltaA](dependency: ReplicationGraph)
      extends ReplicationGraph.HasDependency {
    val deltas: HC.EventSource[(Client, DeltaA)] = HC.Event.source
  }

}
