package mtfrp

package object core {

  import hokko.core

  implicit object ClientBuilder extends HokkoBuilder[ClientTier] {
    def event[A](rep: core.Event[A], graph: ReplicationGraph): ClientTier#Event[A] =
      new ClientEvent(rep, graph)

    def behavior[A](rep: core.Behavior[A], graph: ReplicationGraph): ClientTier#Behavior[A] =
      new ClientBehavior(rep, graph)

    def discreteBehavior[A](
      rep: core.DiscreteBehavior[A],
      initial: A,
      graph: ReplicationGraph
    ): ClientTier#DiscreteBehavior[A] =
      new ClientDiscreteBehavior(rep, initial, graph)

    def incrementalBehavior[A, DeltaA](
      rep: core.IncrementalBehavior[A, DeltaA],
      initial: A,
      graph: ReplicationGraph
    ): ClientTier#IncrementalBehavior[A, DeltaA] =
      new ClientIncBehavior(rep, initial, graph)
  }

  implicit object ServerBuilder extends MockBuilder[ServerTier] {
    def event[A](graph: ReplicationGraph): ServerTier#Event[A] =
      new ServerEvent(graph)

    def behavior[A](graph: ReplicationGraph): ServerTier#Behavior[A] =
      new ServerBehavior(graph)

    def discreteBehavior[A](graph: ReplicationGraph, init: A): ServerTier#DiscreteBehavior[A] =
      new ServerDiscreteBehavior(graph, init)

    def incrementalBehavior[A, DeltaA](
      graph: ReplicationGraph,
      init: A
    ): ServerTier#IncrementalBehavior[A, DeltaA] =
      new ServerIncBehavior(graph, init)
  }
}

package core {
  import hokko.core

  // Define all Client types
  // Create all Client constructors

  class ClientEvent[A] private[core] (
    rep: core.Event[A],
    graph: ReplicationGraph
  ) extends HokkoEvent[ClientTier, A](rep, graph)

  class ClientBehavior[A] private[core] (
    rep: core.Behavior[A],
    graph: ReplicationGraph
  ) extends HokkoBehavior[ClientTier, A](rep, graph)

  class ClientDiscreteBehavior[A] private[core] (
    rep: core.DiscreteBehavior[A],
    initial: A,
    graph: ReplicationGraph
  ) extends HokkoDiscreteBehavior[ClientTier, A](rep, initial, graph)

  class ClientIncBehavior[A, DeltaA] private[core] (
    rep: core.IncrementalBehavior[A, DeltaA],
    initial: A,
    graph: ReplicationGraph
  ) extends HokkoIncBehavior[ClientTier, A, DeltaA](rep, initial, graph)

  final class ClientTier extends HokkoTier with ClientTierLike {
    type T = ClientTier
  }

  object ClientBehavior extends HokkoBehaviorOps[ClientTier]
  object ClientDiscreteBehavior extends HokkoDiscreteBehaviorOps[ClientTier]

  // Define all Server types
  // Create all Server constructors

  class ServerEvent[A] private[core] (graph: ReplicationGraph)
    extends MockEvent[ServerTier, A](graph)
  class ServerBehavior[A] private[core] (graph: ReplicationGraph)
    extends MockBehavior[ServerTier, A](graph)

  class ServerDiscreteBehavior[A] private[core] (
    graph: ReplicationGraph,
    init: A
  ) extends MockDiscreteBehavior[ServerTier, A](init, graph)

  class ServerIncBehavior[A, DeltaA] private[core] (
    graph: ReplicationGraph,
    init: A
  ) extends MockIncBehavior[ServerTier, A, DeltaA](init, graph)

  final class ServerTier extends MockTier with ServerTierLike {
    type T = ServerTier
  }

  object ServerBehavior extends MockBehaviorOps[ServerTier]
  object ServerDiscreteBehavior extends MockDiscreteBehaviorOps[ServerTier]
}
