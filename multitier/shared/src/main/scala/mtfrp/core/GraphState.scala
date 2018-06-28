package mtfrp.core

import cats.kernel.Semigroup
import hokko.core.Engine

case class GraphState(requiresWebSockets: Boolean,
                      replicationGraph: ReplicationGraph,
                      reader: Engine => Unit)

object GraphState {
  private def combine(f: (Boolean, Boolean) => Boolean) =
    new Semigroup[GraphState] {
      def combine(x: GraphState, y: GraphState): GraphState =
        GraphState(f(x.requiresWebSockets, y.requiresWebSockets),
                   x.replicationGraph.combine(y.replicationGraph),
                   (e: Engine) => {
                     x.reader(e); y.reader(e)
                   })
    }

  val all: Semigroup[GraphState] = combine(_ && _)
  val any: Semigroup[GraphState] = combine(_ || _)

  val default = GraphState(false, ReplicationGraph.start, _ => ())
}
