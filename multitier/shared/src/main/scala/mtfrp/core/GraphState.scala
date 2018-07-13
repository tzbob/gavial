package mtfrp.core

import cats.kernel.Semigroup
import hokko.core.Engine

trait GraphState {

  val requiresWebSockets: Boolean
  val replicationGraph: ReplicationGraph
  val effect: Engine => Unit

  val history: Stream[GraphState]

  def copy(
      requiresWebSockets: => Boolean = requiresWebSockets,
      replicationGraph: => ReplicationGraph = replicationGraph,
      effect: => Engine => Unit = effect
  ): GraphState = {
    lazy val rws  = requiresWebSockets
    lazy val rg   = replicationGraph
    lazy val eff  = effect
    lazy val h    = history
    lazy val self = this

    new GraphState {
      lazy val history: Stream[GraphState]        = self #:: h
      lazy val effect: Engine => Unit             = eff
      lazy val replicationGraph: ReplicationGraph = rg
      lazy val requiresWebSockets: Boolean        = rws
    }
  }

  lazy val ws: GraphState  = copy(requiresWebSockets = true)
  lazy val xhr: GraphState = copy(requiresWebSockets = false)

  def mergeGraphAndEffect(other: GraphState): GraphState = {
    val self = this
    new GraphState {
      lazy val history: Stream[GraphState] =
        self #:: other #:: self.history #::: other.history
      lazy val effect: Engine => Unit = (e: Engine) => {
        self.effect(e);
        other.effect(e)
      }
      lazy val replicationGraph: ReplicationGraph =
        self.replicationGraph + other.replicationGraph
      val requiresWebSockets: Boolean = self.requiresWebSockets
    }
  }

  def withEffect(eff: Engine => Unit): GraphState =
    copy(effect = (e: Engine) => {
      effect(e); eff(e)
    })

  def withGraph(replicationGraph: => ReplicationGraph): GraphState =
    copy(replicationGraph = replicationGraph)
}

object GraphState {
  def delayed(graphState: => GraphState): GraphState = new GraphState {
    def shortCutIfLooped[A](f: GraphState => A): A =
      if (this.history contains this) f(GraphState.default)
      else f(graphState)

    lazy val history: Stream[GraphState] = graphState #:: graphState.history
    lazy val effect: Engine => Unit      = shortCutIfLooped(_.effect)

    lazy val replicationGraph: ReplicationGraph = shortCutIfLooped(
      _.replicationGraph)
    lazy val requiresWebSockets: Boolean = shortCutIfLooped(
      _.requiresWebSockets)
  }

  private def combine(f: (Boolean, Boolean) => Boolean) =
    new Semigroup[GraphState] {
      def combine(x: GraphState, y: GraphState): GraphState =
        new GraphState {
          lazy val history: Stream[GraphState] =
            x #:: y #:: x.history #::: y.history
          lazy val effect: Engine => Unit = (e: Engine) => {
            x.effect(e); y.effect(e)
          }
          lazy val replicationGraph: ReplicationGraph =
            x.replicationGraph.combine(y.replicationGraph)
          lazy val requiresWebSockets: Boolean =
            f(x.requiresWebSockets, y.requiresWebSockets)
        }
    }

  val all: Semigroup[GraphState] = combine(_ && _)
  val any: Semigroup[GraphState] = combine(_ || _)

  val default = new GraphState {
    lazy val history: Stream[GraphState]        = Stream.empty
    lazy val effect: Engine => Unit             = _ => ()
    lazy val replicationGraph: ReplicationGraph = ReplicationGraph.start
    lazy val requiresWebSockets: Boolean        = false
  }
}
