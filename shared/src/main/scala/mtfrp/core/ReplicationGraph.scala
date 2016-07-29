package mtfrp
package core

import hokko.{core => HC}

sealed trait ReplicationGraph {
  def combine(graph: ReplicationGraph): ReplicationGraph =
    ReplicationGraph.Combined(List(this, graph))
  def +(graph: ReplicationGraph): ReplicationGraph =
    combine(graph)
}

case class ExitEvent[A](event: HC.Event[A])
case class ExitBehavior[A, DeltaA](behavior: HC.IncrementalBehavior[A, DeltaA])

case class InputEvent[A](event: HC.EventSource[A])

object ReplicationGraph {
  def exitEvents(graph: ReplicationGraph): List[ExitEvent[_]] = {
    ReplicationGraph.toList(graph).collect {
      case SenderEvent(event, _) => ExitEvent(event)
    }
  }

  def exitBehaviors(graph: ReplicationGraph): List[ExitBehavior[_, _]] = {
    ReplicationGraph.toList(graph).collect {
      case SenderBehavior(beh, _) => ExitBehavior(beh)
    }
  }

  def inputEvents(graph: ReplicationGraph): List[InputEvent[_]] = {
    ReplicationGraph.toList(graph).collect {
      case ReceiverEvent(source, _) => InputEvent(source)
    }
  }

  private def toList(graph: ReplicationGraph): List[ReplicationGraph] = {
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

  def eventSender[A](dep: ReplicationGraph, evt: HC.Event[Client => Option[A]]): ReplicationGraph =
    ReplicationGraph.SenderEvent(evt, dep)

  def behaviorSender[A, DeltaA](
    dep: ReplicationGraph,
    beh: HC.IncrementalBehavior[Client => A, Client => Option[DeltaA]]
  ): ReplicationGraph =
    ReplicationGraph.SenderBehavior(beh, dep)

  def eventReceiver[A](dep: ReplicationGraph, evt: HC.EventSource[A]): ReplicationGraph =
    ReplicationGraph.ReceiverEvent(evt, dep)

  def behaviorReceiver[A, DeltaA](
    dep: ReplicationGraph,
    // TODO
    folder: HC.EventSource[Any]
  ): ReplicationGraph =
    ReplicationGraph.ReceiverBehavior(folder, dep)

  case object start extends ReplicationGraph

  sealed trait HasDependency extends ReplicationGraph {
    val dependency: ReplicationGraph
  }

  private case class Combined(
    nodes: Seq[ReplicationGraph]
  ) extends ReplicationGraph

  private case class ReceiverEvent[A](
    source: HC.EventSource[A],
    dependency: ReplicationGraph
  ) extends ReplicationGraph with HasDependency

  private case class ReceiverBehavior[A, DeltaA](
    // TODO
    folder: HC.EventSource[Any],
    dependency: ReplicationGraph
  ) extends ReplicationGraph with HasDependency

  private case class SenderEvent[A](
    event: HC.Event[Client => Option[A]],
    dependency: ReplicationGraph
  ) extends ReplicationGraph with HasDependency

  private case class SenderBehavior[A, DeltaA](
    event: HC.IncrementalBehavior[A, DeltaA],
    dependency: ReplicationGraph
  ) extends ReplicationGraph with HasDependency
}
