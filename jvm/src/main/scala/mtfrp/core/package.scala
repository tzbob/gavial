package mtfrp

package object core {

  import hokko.core

  implicit object AppBuilder extends HokkoBuilder[AppTier] {
    def event[A](rep: core.Event[A],
                 graph: ReplicationGraph): AppTier#Event[A] =
      new AppEvent(rep, graph)

    def behavior[A](rep: core.Behavior[A],
                    graph: ReplicationGraph): AppTier#Behavior[A] =
      new AppBehavior(rep, graph)

    def discreteBehavior[A](
        rep: core.DiscreteBehavior[A],
        initial: A,
        graph: ReplicationGraph
    ): AppTier#DiscreteBehavior[A] =
      new AppDiscreteBehavior(rep, initial, graph)

    def incrementalBehavior[A, DeltaA](
        rep: core.IncrementalBehavior[A, DeltaA],
        initial: A,
        graph: ReplicationGraph,
        accumulator: (A, DeltaA) => A
    ): AppTier#IncrementalBehavior[A, DeltaA] =
      new AppIncBehavior(rep, initial, graph, accumulator)
  }

  implicit object ClientBuilder extends MockBuilder[ClientTier] {
    def event[A](graph: ReplicationGraph): ClientTier#Event[A] =
      new ClientEvent(graph)

    def behavior[A](graph: ReplicationGraph): ClientTier#Behavior[A] =
      new ClientBehavior(graph)

    def discreteBehavior[A](graph: ReplicationGraph,
                            initial: A): ClientTier#DiscreteBehavior[A] =
      new ClientDiscreteBehavior(graph, initial)

    def incrementalBehavior[A, DeltaA](
        graph: ReplicationGraph,
        accumulator: (A, DeltaA) => A,
        initial: A
    ): ClientTier#IncrementalBehavior[A, DeltaA] =
      new ClientIncBehavior(graph, accumulator, initial)
  }
}
