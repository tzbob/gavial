package mtfrp.core

import cats.kernel.Semigroup
import hokko.core.Engine

case class GraphState(requiresWebSockets: Boolean,
                      replicationGraph: ReplicationGraph,
                      effect: Engine => Unit) {
  lazy val ws: GraphState  = copy(requiresWebSockets = true)
  lazy val xhr: GraphState = copy(requiresWebSockets = false)

  def mergeGraphAndEffect(other: GraphState): GraphState =
    copy(replicationGraph = this.replicationGraph + other.replicationGraph,
         effect = (e: Engine) => {
           this.effect(e); other.effect(e)
         })

  def withEffect(eff: Engine => Unit): GraphState =
    copy(effect = (e: Engine) => { effect(e); eff(e) })
}

object GraphState {
  private def combine(f: (Boolean, Boolean) => Boolean) =
    new Semigroup[GraphState] {
      def combine(x: GraphState, y: GraphState): GraphState =
        GraphState(f(x.requiresWebSockets, y.requiresWebSockets),
                   x.replicationGraph.combine(y.replicationGraph),
                   (e: Engine) => {
                     x.effect(e); y.effect(e)
                   })
    }

  val all: Semigroup[GraphState] = combine(_ && _)
  val any: Semigroup[GraphState] = combine(_ || _)

  val default = GraphState(false, ReplicationGraph.start, _ => ())
}
