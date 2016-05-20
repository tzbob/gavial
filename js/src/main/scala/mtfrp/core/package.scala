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

  implicit object ApplicationBuilder extends MockBuilder[ApplicationTier] {
    def event[A](graph: ReplicationGraph): ApplicationTier#Event[A] =
      new ApplicationEvent(graph)

    def behavior[A](graph: ReplicationGraph): ApplicationTier#Behavior[A] =
      new ApplicationBehavior(graph)

    def discreteBehavior[A](graph: ReplicationGraph, init: A): ApplicationTier#DiscreteBehavior[A] =
      new ApplicationDiscreteBehavior(graph, init)

    def incrementalBehavior[A, DeltaA](
      graph: ReplicationGraph,
      init: A
    ): ApplicationTier#IncrementalBehavior[A, DeltaA] =
      new ApplicationIncBehavior(graph, init)
  }
}
