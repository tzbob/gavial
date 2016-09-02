package mtfrp.core

import hokko.{core => HC}
import io.circe.{Decoder, Encoder}
import mtfrp.core.ReplicationGraph.{Pulse, PulseMaker}
import mtfrp.core.ReplicationGraphClient.ReceiverEvent

class ReplicationGraphClient(graph: ReplicationGraph) {
  val graphList = ReplicationGraph.toList(graph)

  val inputEventRouter: PulseMaker = {
    val receivers: Map[Int, PulseMaker] = graphList.collect {
      case r: ReceiverEvent[_] => (r.token, r.pulse _)
    }.toMap

    (msg: Message) =>
      receivers(msg.id)(msg)
  }

  def exitEvent: HC.Event[Seq[Message]] = {
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

  case class ReceiverEvent[A: Decoder](
      source: HC.EventSource[A],
      dependency: ReplicationGraph
  ) extends ReplicationGraph.HasDependency {
    def pulse(msg: Message): Option[Pulse] = {
      val decoded = msg.payload.as[A]
      decoded.toOption.map(source.->)
    }
  }

  case class ReceiverBehavior[A, DeltaA](
      deltas: HC.EventSource[DeltaA],
      resets: HC.EventSource[A],
      dependency: ReplicationGraph
  ) extends ReplicationGraph.HasDependency

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
    // TODO: do we need this?
    val message: HC.Behavior[Message] =
      behavior.map(Message.fromPayload(this.token))
  }
}