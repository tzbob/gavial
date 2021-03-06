package mtfrp

import hokko.core.Thunk
import mtfrp.core.impl.HokkoBuilder
import mtfrp.core.mock.MockBuilder

package object core {

  import hokko.core

  implicit object AppBuilder extends HokkoBuilder[AppTier] {
    def event[A](rep: core.Event[A], graph: GraphState): AppTier#Event[A] =
      new AppEvent(rep, graph)

    def behavior[A](rep: core.CBehavior[A],
                    graph: GraphState): AppTier#Behavior[A] =
      new AppBehavior(rep, graph)

    def DBehavior[A](
        rep: core.DBehavior[A],
        graph: => GraphState
    ): AppTier#DBehavior[A] =
      new AppDBehavior(rep, graph)

    def IBehavior[A, DeltaA](
        rep: core.IBehavior[A, DeltaA],
        graph: GraphState
    ): AppTier#IBehavior[A, DeltaA] =
      new AppIBehavior(rep, graph)
  }

  implicit object ClientBuilder extends MockBuilder[ClientTier] {
    def event[A](graph: GraphState): ClientTier#Event[A] =
      new ClientEvent(graph)

    def behavior[A](graph: GraphState,
                    initial: Thunk[A]): ClientTier#Behavior[A] =
      new ClientBehavior(graph, initial)

    def DBehavior[A](graph: => GraphState,
                     initial: => A): ClientTier#DBehavior[A] =
      new ClientDBehavior(graph, initial)

    def IBehavior[A, DeltaA](
        graph: GraphState,
        accumulator: (A, DeltaA) => A,
        initial: A
    ): ClientTier#IBehavior[A, DeltaA] =
      new ClientIBehavior(graph, accumulator, initial)
  }
}
