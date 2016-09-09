package mtfrp
package core

import hokko.{core => HC}

case class ExitData(
    event: HC.Event[Client => Seq[Message]],
    behavior: HC.CBehavior[Client => Seq[Message]]
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
  type Pulse = (HC.EventSource[T], T) forSome { type T }

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

  case object start extends ReplicationGraph

  trait HasDependency extends ReplicationGraph with HasToken {
    val dependency: ReplicationGraph
  }

  private case class Combined(
      nodes: Seq[ReplicationGraph]
  ) extends ReplicationGraph

  trait EventServerToClient extends HasDependency
  trait EventClientToServer extends HasDependency

  trait BehaviorServerToClient extends HasDependency {
    val deltas: HasDependency
  }

  trait BehaviorClientToServer extends HasDependency {
    val deltas: HasDependency
  }
}
