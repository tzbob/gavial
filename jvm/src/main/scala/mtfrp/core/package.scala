package mtfrp

package object core {

  import hokko.core

  implicit object ApplicationBuilder extends HokkoBuilder[ApplicationTier] {
    def event[A](rep: core.Event[A], graph: ReplicationGraph): ApplicationTier#Event[A] =
      new ApplicationEvent(rep, graph)

    def behavior[A](rep: core.Behavior[A], graph: ReplicationGraph): ApplicationTier#Behavior[A] =
      new ApplicationBehavior(rep, graph)

    def discreteBehavior[A](
      rep: core.DiscreteBehavior[A],
      initial: A,
      graph: ReplicationGraph
    ): ApplicationTier#DiscreteBehavior[A] =
      new ApplicationDiscreteBehavior(rep, initial, graph)

    def incrementalBehavior[A, DeltaA](
      rep: core.IncrementalBehavior[A, DeltaA],
      initial: A,
      graph: ReplicationGraph
    ): ApplicationTier#IncrementalBehavior[A, DeltaA] =
      new ApplicationIncBehavior(rep, initial, graph)
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
