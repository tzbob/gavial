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

  def exitEvent(
      graph: List[ReplicationGraph]
  ): HC.Event[Client => Seq[Message]] = {
    // all senders that should be added to the exit event (events and deltas)
    val senders = graph.collect {
      case s: SenderEvent[_]       => s.message
      case s: SenderBehavior[_, _] => s.deltaSender.message
    }

    val mergedSenders = HC.Event.merge(senders.to[Seq])

    val mergedSendersOneClient = mergedSenders.map { evfs => (c: Client) =>
      // make a client function that finds all messages
      evfs.map { evf =>
        evf(c)
      }.flatten
    }
    mergedSendersOneClient
  }

  def exitBehavior(
      graph: List[ReplicationGraph]
  ): HC.Behavior[Client => Seq[Message]] = {
    // all senders that should be added to the exit behavior
    val senders = graph.collect {
      case s: SenderBehavior[_, _] => s.message
    }

    val mergedSenders =
      senders.foldLeft(HC.Behavior.constant(List.empty[Client => Message])) {
        (accB, newB) =>
          accB.map2(newB)(_ :+ _)
      }

    val mergedSendersOneClient = mergedSenders.map { evfs => (c: Client) =>
      // make a client function that finds all messages
      evfs.map { evf =>
        evf(c)
    }
    }
    mergedSendersOneClient
  }

  def exitData(graph: ReplicationGraph): ExitData = {
    // all senders that should be added to the exit event (events and deltas)
    val graphList = ReplicationGraph.toList(graph)
    ExitData(exitEvent(graphList), exitBehavior(graphList))
  }

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

  def eventSender[A: Encoder](
      dep: ReplicationGraph,
      evt: HC.Event[Client => Option[A]]
  ): ReplicationGraph =
    ReplicationGraph.SenderEvent(evt, dep)

  def behaviorSender[A: Encoder, DeltaA: Encoder](
      dep: ReplicationGraph,
      beh: HC.IncrementalBehavior[Client => A, Client => Option[DeltaA]]
  ): ReplicationGraph =
    ReplicationGraph.SenderBehavior(beh, dep)

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

  sealed trait HasDependency extends ReplicationGraph with HasToken {
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

  private case class SenderEvent[A: Encoder](
      event: HC.Event[Client => Option[A]],
      dependency: ReplicationGraph
  ) extends ReplicationGraph
      with HasDependency {
    val message = event.map { evf => c: Client =>
      evf(c).map(Message.fromPayload(this.token))
    }
  }

  private case class SenderBehavior[A: Encoder, DeltaA: Encoder](
      behavior: HC.IncrementalBehavior[Client => A, Client => Option[DeltaA]],
      dependency: ReplicationGraph
  ) extends ReplicationGraph
      with HasDependency {
    val deltaSender = SenderEvent(behavior.deltas, dependency)
    val message = behavior.map { evf => c: Client =>
      Message.fromPayload(this.token)(evf(c))
    }
  }
}
