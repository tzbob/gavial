package mtfrp

import mtfrp.core.impl.HokkoBuilder
import mtfrp.core.mock.MockBuilder

package object core {

  import hokko.core

  implicit object ClientBuilder extends HokkoBuilder[ClientTier] {
    def event[A](rep: core.Event[A],
                 graph: ReplicationGraph,
                 requiresWebSockets: Boolean): ClientTier#Event[A] =
      new ClientEvent(rep, graph, requiresWebSockets)

    def behavior[A](rep: core.CBehavior[A],
                    graph: ReplicationGraph,
                    requiresWebSockets: Boolean): ClientTier#Behavior[A] =
      new ClientBehavior(rep, graph, requiresWebSockets)

    def DBehavior[A](
        rep: core.DBehavior[A],
        initial: A,
        graph: ReplicationGraph,
        requiresWebSockets: Boolean
    ): ClientTier#DBehavior[A] =
      new ClientDBehavior(rep, initial, graph, requiresWebSockets)

    def IBehavior[A, DeltaA](
        rep: core.IBehavior[A, DeltaA],
        initial: A,
        graph: ReplicationGraph,
        accumulator: (A, DeltaA) => A,
        requiresWebSockets: Boolean
    ): ClientTier#IBehavior[A, DeltaA] =
      new ClientIBehavior(rep, initial, graph, accumulator, requiresWebSockets)
  }

  implicit object AppBuilder extends MockBuilder[AppTier] {
    def event[A](graph: ReplicationGraph,
                 requiresWebSockets: Boolean): AppTier#Event[A] =
      new AppEvent(graph, requiresWebSockets)

    def behavior[A](graph: ReplicationGraph,
                    requiresWebSockets: Boolean): AppTier#Behavior[A] =
      new AppBehavior(graph, requiresWebSockets)

    def DBehavior[A](graph: ReplicationGraph,
                     initial: A,
                     requiresWebSockets: Boolean): AppTier#DBehavior[A] =
      new AppDBehavior(graph, initial, requiresWebSockets)

    def IBehavior[A, DeltaA](
        graph: ReplicationGraph,
        accumulator: (A, DeltaA) => A,
        initial: A,
        requiresWebSockets: Boolean
    ): AppTier#IBehavior[A, DeltaA] =
      new AppIBehavior(graph, accumulator, initial, requiresWebSockets)
  }
}