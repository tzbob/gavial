package mtfrp

package object core {

  import hokko.core

  implicit object ServerBuilder extends HokkoBuilder[ServerTier] {
    def event[A](rep: core.Event[A], graph: ReplicationGraph): ServerTier#Event[A] =
      new ServerEvent(rep, graph)

    def behavior[A](rep: core.Behavior[A], graph: ReplicationGraph): ServerTier#Behavior[A] =
      new ServerBehavior(rep, graph)

    def discreteBehavior[A](
      rep: core.DiscreteBehavior[A],
      initial: A,
      graph: ReplicationGraph
    ): ServerTier#DiscreteBehavior[A] =
      new ServerDiscreteBehavior(rep, initial, graph)

    def incrementalBehavior[A, DeltaA](
      rep: core.IncrementalBehavior[A, DeltaA],
      initial: A,
      graph: ReplicationGraph
    ): ServerTier#IncrementalBehavior[A, DeltaA] =
      new ServerIncBehavior(rep, initial, graph)
  }

  implicit object ClientBuilder extends MockBuilder[ClientTier] {
    def event[A](graph: ReplicationGraph): ClientTier#Event[A] =
      new ClientEvent(graph)

    def behavior[A](graph: ReplicationGraph): ClientTier#Behavior[A] =
      new ClientBehavior(graph)

    def discreteBehavior[A](graph: ReplicationGraph, init: A): ClientTier#DiscreteBehavior[A] =
      new ClientDiscreteBehavior(graph, init)

    def incrementalBehavior[A, DeltaA](
      graph: ReplicationGraph,
      init: A
    ): ClientTier#IncrementalBehavior[A, DeltaA] =
      new ClientIncBehavior(graph, init)
  }
}

package core {
  import hokko.core

  // Define all Server types
  // Create all Server constructors

  class ServerEvent[A] private[core] (
    rep: core.Event[A],
    graph: ReplicationGraph
  ) extends HokkoEvent[ServerTier, A](rep, graph)

  class ServerBehavior[A] private[core] (
    rep: core.Behavior[A],
    graph: ReplicationGraph
  ) extends HokkoBehavior[ServerTier, A](rep, graph)

  class ServerDiscreteBehavior[A] private[core] (
    rep: core.DiscreteBehavior[A],
    initial: A,
    graph: ReplicationGraph
  ) extends HokkoDiscreteBehavior[ServerTier, A](rep, initial, graph)

  class ServerIncBehavior[A, DeltaA] private[core] (
    rep: core.IncrementalBehavior[A, DeltaA],
    initial: A,
    graph: ReplicationGraph
  ) extends HokkoIncBehavior[ServerTier, A, DeltaA](rep, initial, graph)

  final class ServerTier extends HokkoTier with ServerTierLike {
    type T = ServerTier
  }

  object ServerBehavior extends HokkoBehaviorOps[ServerTier]
  object ServerDiscreteBehavior extends HokkoDiscreteBehaviorOps[ServerTier]

  // Define all Client types
  // Create all Client constructors

  class ClientEvent[A] private[core] (graph: ReplicationGraph)
    extends MockEvent[ClientTier, A](graph)
  class ClientBehavior[A] private[core] (graph: ReplicationGraph)
    extends MockBehavior[ClientTier, A](graph)

  class ClientDiscreteBehavior[A] private[core] (
    graph: ReplicationGraph,
    init: A
  ) extends MockDiscreteBehavior[ClientTier, A](init, graph)

  class ClientIncBehavior[A, DeltaA] private[core] (
    graph: ReplicationGraph,
    init: A
  ) extends MockIncBehavior[ClientTier, A, DeltaA](init, graph)

  final class ClientTier extends MockTier with ClientTierLike {
    type T = ClientTier
  }

  object ClientBehavior extends MockBehaviorOps[ClientTier]
  object ClientDiscreteBehavior extends MockDiscreteBehaviorOps[ClientTier]
}
