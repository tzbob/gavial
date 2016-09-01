package mtfrp.core

import hokko.{core => HC}
import io.circe.Decoder
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
}

object ReplicationGraphClient {
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
}
