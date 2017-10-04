package mtfrp.core

import hokko.{core => HC}
import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder}
import mtfrp.core.ReplicationGraph.Pulse
import mtfrp.core.ReplicationGraphClient.{ReceiverBehavior, ReceiverEvent}

class ReplicationGraphClient(graph: ReplicationGraph) {
  val graphList = ReplicationGraph.toList(graph)

  val inputEventRouter: Message => Option[Pulse] = {
    val receivers =
      graphList.foldLeft(Map.empty[Int, Message => Option[Pulse]]) {
        (acc, node) =>
          node match {
            case r: ReceiverEvent[_] => acc + (r.token -> r.pulse _)
            case r: ReceiverBehavior[_, _] =>
              val deltaAcc = acc + (r.deltas.token -> r.deltas.pulse _)
              deltaAcc + (r.token -> r.pulse _)
            case _ => acc
          }
      }

    (msg: Message) =>
      receivers.get(msg.id).flatMap(_ apply msg)
  }

  val exitEvent: HC.Event[Seq[Message]] = {
    // all senders that should be added to the exit event (events and deltas)
    val senders = graphList.collect {
      case s: ReplicationGraphClient.SenderEvent[_] =>
        s.message
      case s: ReplicationGraphClient.SenderBehavior[_, _] =>
        s.deltas.message
    }
    HC.Event.merge(senders)
  }
}

object ReplicationGraphClient {
  case class ExitData(
      event: HC.Event[Seq[Message]],
      behavior: HC.CBehavior[Seq[Message]]
  )

  def pulse[A: Decoder](source: HC.EventSource[A],
                        msg: Message): Option[Pulse] = {
    val decoded: Result[A] = msg.payload.as[A]
    decoded.right.toOption.map(source.->)
  }

  case class ReceiverEvent[A: Decoder](dependency: ReplicationGraph)
      extends ReplicationGraph.EventServerToClient {
    val source: HC.EventSource[A] = HC.Event.source
    def pulse(msg: Message): Option[Pulse] =
      ReplicationGraphClient.pulse(source, msg)
  }

  case class ReceiverBehavior[A: Decoder, DeltaA: Decoder](
      dependency: ReplicationGraph)
      extends ReplicationGraph.BehaviorServerToClient {
    override val deltas: ReceiverEvent[DeltaA] =
      ReceiverEvent[DeltaA](dependency)
    val resets = HC.Event.source[A]
    def pulse(msg: Message): Option[Pulse] =
      ReplicationGraphClient.pulse(resets, msg)
  }

  case class SenderEvent[A: Encoder](
      event: HC.Event[A],
      dependency: ReplicationGraph
  ) extends ReplicationGraph.EventClientToServer {
    val message: HC.Event[Message] = event.map(Message.fromPayload(this.token))
  }

  case class SenderBehavior[A: Encoder, DeltaA: Encoder](
      behavior: HC.IBehavior[A, DeltaA],
      dependency: ReplicationGraph
  ) extends ReplicationGraph.BehaviorClientToServer {
    override val deltas: SenderEvent[DeltaA] =
      SenderEvent(behavior.deltas, dependency)
  }
}
