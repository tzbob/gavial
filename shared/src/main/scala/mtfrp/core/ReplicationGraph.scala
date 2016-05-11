package mtfrp
package core

import hokko.{core => HC}

sealed trait ReplicationGraph {
  def combine(graph: ReplicationGraph): ReplicationGraph =
    ReplicationGraph.Combined(List(this, graph))
  def +(graph: ReplicationGraph): ReplicationGraph =
    combine(graph)
}

object ReplicationGraph {
  def combine(graphs: Seq[ReplicationGraph]): ReplicationGraph =
    ReplicationGraph.Combined(graphs)

  def sender[A](dep: ReplicationGraph, evt: HC.EventSource[A]): ReplicationGraph =
    ReplicationGraph.Event(evt, Some(dep), Sender)

  def sender[A, DeltaA](
    dep: ReplicationGraph,
    beh: HC.IncrementalBehavior[A, DeltaA]
  ): ReplicationGraph =
    ReplicationGraph.IncrementalBehavior(beh, Some(dep), Sender)

  def receiver[A](dep: ReplicationGraph, evt: HC.Event[A]): ReplicationGraph =
    ReplicationGraph.Event(evt, Some(dep), Receiver)

  def receiver[A, DeltaA](
    dep: ReplicationGraph,
    beh: HC.IncrementalBehavior[A, DeltaA]
  ): ReplicationGraph =
    ReplicationGraph.IncrementalBehavior(beh, Some(dep), Receiver)

  case object start extends ReplicationGraph

  private case class Combined(
    nodes: Seq[ReplicationGraph]
  ) extends ReplicationGraph

  private sealed trait Role
  private case object Sender extends Role
  private case object Receiver extends Role

  private case class Event[A](
    event: HC.Event[A],
    dependency: Option[ReplicationGraph],
    role: Role
  ) extends ReplicationGraph

  private case class IncrementalBehavior[A, DeltaA](
    behavior: HC.IncrementalBehavior[A, DeltaA],
    dependency: Option[ReplicationGraph],
    role: Role
  ) extends ReplicationGraph

}
