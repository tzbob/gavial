package mtfrp

package object core {

  import hokko.core

  implicit object ClientBuilder extends HokkoBuilder[ClientTier] {
    def event[A](rep: core.Event[A],
                 graph: ReplicationGraph): ClientTier#Event[A] =
      new ClientEvent(rep, graph)

    def behavior[A](rep: core.CBehavior[A],
                    graph: ReplicationGraph): ClientTier#Behavior[A] =
      new ClientBehavior(rep, graph)

    def discreteBehavior[A](
        rep: core.DBehavior[A],
        initial: A,
        graph: ReplicationGraph
    ): ClientTier#DiscreteBehavior[A] =
      new ClientDiscreteBehavior(rep, initial, graph)

    def incrementalBehavior[A, DeltaA](
        rep: core.IBehavior[A, DeltaA],
        initial: A,
        graph: ReplicationGraph,
        accumulator: (A, DeltaA) => A
    ): ClientTier#IncrementalBehavior[A, DeltaA] =
      new ClientIncBehavior(rep, initial, graph, accumulator)
  }

  implicit object AppBuilder extends MockBuilder[AppTier] {
    def event[A](graph: ReplicationGraph): AppTier#Event[A] =
      new AppEvent(graph)

    def behavior[A](graph: ReplicationGraph): AppTier#Behavior[A] =
      new AppBehavior(graph)

    def discreteBehavior[A](graph: ReplicationGraph,
                            initial: A): AppTier#DiscreteBehavior[A] =
      new AppDiscreteBehavior(graph, initial)

    def incrementalBehavior[A, DeltaA](
        graph: ReplicationGraph,
        accumulator: (A, DeltaA) => A,
        initial: A
    ): AppTier#IncrementalBehavior[A, DeltaA] =
      new AppIncBehavior(graph, accumulator, initial)
  }
}
