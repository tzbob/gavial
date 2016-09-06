package mtfrp.core

import hokko.{core => HC}
import io.circe.{Decoder, Encoder}
import mtfrp.core.ReplicationGraph.Pulse
import mtfrp.core.ReplicationGraphClient.ReceiverEvent

class ReplicationGraphClient(graph: ReplicationGraph) {
  val graphList = ReplicationGraph.toList(graph)

  val inputEventRouter: Message => Option[Pulse] = {
    val receivers: Map[Int, Message => Option[Pulse]] = graphList.collect {
      case r: ReceiverEvent[_] => (r.token, r.pulse _)
    }.toMap

    (msg: Message) =>
      receivers.get(msg.id).flatMap(_ apply msg)
  }

  val exitEvent: HC.Event[Seq[Message]] = {
    // all senders that should be added to the exit event (events and deltas)
    val senders = graphList.collect {
      case s: ReplicationGraphClient.SenderEvent[_] =>
        s.message
      case s: ReplicationGraphClient.SenderBehavior[_, _] =>
        s.deltaSender.message
    }
    HC.Event.merge(senders)
  }
}

object ReplicationGraphClient {
  case class ExitData(
      event: HC.Event[Seq[Message]],
      behavior: HC.Behavior[Seq[Message]]
  )

  case class ReceiverEvent[A: Decoder](dependency: ReplicationGraph)
      extends ReplicationGraph.HasDependency {
    val source: HC.EventSource[A] = HC.Event.source
    def pulse(msg: Message): Option[Pulse] = {
      val decoded = msg.payload.as[A]
      decoded.toOption.map(source.->)
    }
  }

  case class ReceiverBehavior[A, DeltaA](dependency: ReplicationGraph)
      extends ReplicationGraph.HasDependency {
    val deltas = HC.Event.source[DeltaA]
    val resets = HC.Event.source[A]
  }

  case class SenderEvent[A: Encoder](
      event: HC.Event[A],
      dependency: ReplicationGraph
  ) extends ReplicationGraph.HasDependency {
    val message: HC.Event[Message] = event.map(Message.fromPayload(this.token))
  }

  case class SenderBehavior[A: Encoder, DeltaA: Encoder](
      behavior: HC.IncrementalBehavior[A, DeltaA],
      dependency: ReplicationGraph
  ) extends ReplicationGraph.HasDependency {
    val deltaSender = SenderEvent(behavior.deltas, dependency)
  }
}
