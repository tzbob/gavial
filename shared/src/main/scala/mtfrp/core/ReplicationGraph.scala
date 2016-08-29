package mtfrp
package core

import hokko.{core => HC}
import io.circe._

case class ExitData(
    event: HC.Event[Client => Seq[Message]],
    behavior: HC.Behavior[Client => Seq[Message]]
)

case class InputEventRouter(inputs: Map[Int, HC.EventSource[_]])

///////////////////////////////////////////////
///////////////////////////////////////////////

sealed trait ReplicationGraph {
  def combine(graph: ReplicationGraph): ReplicationGraph =
    ReplicationGraph.Combined(List(this, graph))
  def +(graph: ReplicationGraph): ReplicationGraph =
    combine(graph)
}

object ReplicationGraph {
  type Pulse      = (HC.EventSource[T], T) forSome { type T }
  type PulseMaker = Message => Option[Pulse]

  def inputEventRouter(graph: ReplicationGraph): PulseMaker = {
    val receivers: Map[Int, PulseMaker] = ReplicationGraph
      .toList(graph)
      .collect {
        case r: ReceiverEvent[_] => (r.token, r.pulse _)
      }
      .toMap

    (msg: Message) =>
      receivers(msg.id)(msg)
  }

  private[core] def toList(graph: ReplicationGraph): List[ReplicationGraph] = {
    val rest = graph match {
      case `start` =>
        Nil
      case Combined(nodes) =>
        nodes.map(ReplicationGraph.toList).toList.flatten
      case e: HasDependency =>
        ReplicationGraph.toList(e.dependency)
    }
    graph :: rest
  }

  def combine(graphs: Seq[ReplicationGraph]): ReplicationGraph =
    ReplicationGraph.Combined(graphs)

  def eventReceiver[A: Decoder](
      dep: ReplicationGraph,
      evt: HC.EventSource[A]
  ): ReplicationGraph =
    ReplicationGraph.ReceiverEvent(evt, dep)

  def behaviorReceiver[A, DeltaA](
      dep: ReplicationGraph,
      // TODO
      deltas: HC.EventSource[DeltaA],
      resets: HC.EventSource[A]
  ): ReplicationGraph =
    ReplicationGraph.ReceiverBehavior(deltas, resets, dep)

  case object start extends ReplicationGraph

  trait HasDependency extends ReplicationGraph with HasToken {
    val dependency: ReplicationGraph
  }

  private case class Combined(
      nodes: Seq[ReplicationGraph]
  ) extends ReplicationGraph

  private case class ReceiverEvent[A: Decoder](
      source: HC.EventSource[A],
      dependency: ReplicationGraph
  ) extends ReplicationGraph
      with HasDependency {
    def pulse(msg: Message): Option[Pulse] = {
      val decoded = msg.payload.as[A]
      decoded.toOption.map(source.->)
    }
  }

  private case class ReceiverBehavior[A, DeltaA](
      // TODO
      deltas: HC.EventSource[DeltaA],
      resets: HC.EventSource[A],
      dependency: ReplicationGraph
  ) extends ReplicationGraph
      with HasDependency

}
