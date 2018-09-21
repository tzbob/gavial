package mtfrp

import hokko.core.Thunk
import mtfrp.core.impl.HokkoBuilder
import mtfrp.core.mock.MockBuilder

package object core {

  import hokko.core

  implicit object ClientBuilder extends HokkoBuilder[ClientTier] {
    def event[A](rep: core.Event[A], graph: GraphState): ClientTier#Event[A] =
      new ClientEvent(rep, graph)

    def behavior[A](rep: core.CBehavior[A],
                    graph: GraphState): ClientTier#Behavior[A] =
      new ClientBehavior(rep, graph)

    def DBehavior[A](
        rep: core.DBehavior[A],
        graph: => GraphState
    ): ClientTier#DBehavior[A] =
      new ClientDBehavior(rep, graph)

    def IBehavior[A, DeltaA](
        rep: core.IBehavior[A, DeltaA],
        graph: GraphState,
    ): ClientTier#IBehavior[A, DeltaA] =
      new ClientIBehavior(rep, graph)
  }

  implicit object AppBuilder extends MockBuilder[AppTier] {
    def event[A](graph: GraphState): AppTier#Event[A] =
      new AppEvent(graph)

    def behavior[A](graph: GraphState, initial: Thunk[A]): AppTier#Behavior[A] =
      new AppBehavior(graph, initial)

    def DBehavior[A](graph: => GraphState,
                     initial: => A): AppTier#DBehavior[A] =
      new AppDBehavior(graph, initial)

    def IBehavior[A, DeltaA](
        graph: GraphState,
        accumulator: (A, DeltaA) => A,
        initial: A
    ): AppTier#IBehavior[A, DeltaA] =
      new AppIBehavior(graph, accumulator, initial)
  }
}
