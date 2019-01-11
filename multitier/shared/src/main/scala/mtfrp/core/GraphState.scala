package mtfrp.core

import cats.Eval
import cats.kernel.Semigroup
import hokko.core.Engine

import cats.implicits._

case class GraphState(
    requiresWebSockets: Eval[Boolean],
    replicationGraph: Eval[ReplicationGraph],
    effect: Eval[Set[Engine => Unit]],
    history: Eval[Stream[GraphState]]
) {

  lazy val ws: GraphState =
    copy(requiresWebSockets = Eval.now(true),
         history = Eval.later(this #:: history.value))

  lazy val xhr: GraphState =
    copy(Eval.now(false), history = Eval.later(this #:: history.value))

  def mergeGraphAndEffect(other: GraphState): GraphState =
    GraphState.any.combine(this, other)

  def withEffect(eff: Eval[Engine => Unit]): GraphState =
    copy(effect = effect.map2(eff) { _ + _ },
         history = Eval.later(this #:: history.value))

  def withGraph(replicationGraph: Eval[ReplicationGraph]): GraphState =
    copy(replicationGraph = replicationGraph,
         history = Eval.later(this #:: history.value))
}

object GraphState {
  def delayed(graphState: => GraphState): GraphState = {
    lazy val delayedState = graphState
    lazy val delayedWrapper: GraphState = GraphState(
      Eval.defer(shortCut(_.requiresWebSockets)),
      Eval.defer(shortCut(_.replicationGraph)),
      Eval.defer(shortCut(_.effect)),
      Eval.defer(Eval.later(delayedState #:: delayedState.history.value))
    )

    def shortCut[A](f: GraphState => Eval[A]) = Eval.later {
      if (delayedState.history.value.contains(delayedWrapper))
        f(GraphState.default).value
      else f(delayedState).value
    }

    delayedWrapper
  }

  private def combine(f: (Boolean, Boolean) => Boolean) =
    new Semigroup[GraphState] {
      def combine(x: GraphState, y: GraphState): GraphState =
        GraphState(
          x.requiresWebSockets.map2(y.requiresWebSockets)(f),
          x.replicationGraph.map2(y.replicationGraph)(_ + _),
          x.effect.map2(y.effect) { _ ++ _ },
          x.history.map2(y.history)((xh, yh) =>
            x #:: y #:: breadthFirstStreamCombine(xh, yh))
        )

      private def breadthFirstStreamCombine[A](a: Stream[A],
                                               b: Stream[A]): Stream[A] =
        a match {
          case first #:: rest => first #:: breadthFirstStreamCombine(b, rest)
          case _              => b
        }
    }

  val any: Semigroup[GraphState] = combine(_ || _)

  val default = GraphState(
    Eval.now(false),
    Eval.now(ReplicationGraph.start),
    Eval.now(Set.empty),
    Eval.now(Stream.empty)
  )
}
