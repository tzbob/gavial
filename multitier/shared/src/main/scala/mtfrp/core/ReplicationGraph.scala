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
  import scala.language.existentials
  type Pulse = (HC.EventSource[T], T) forSome { type T }

  private[core] def toSet(graph: ReplicationGraph): Set[ReplicationGraph] = {
    val rest = graph match {
      case `start` =>
        Set.empty[ReplicationGraph]
      case Combined(nodes) =>
        nodes.map(ReplicationGraph.toSet).flatten.toSet
      case e: HasDependency =>
        ReplicationGraph.toSet(e.dependency)
    }
    rest + graph
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
