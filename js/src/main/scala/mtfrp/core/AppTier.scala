package mtfrp
package core

import hokko.core
import io.circe._

import cats.data.Xor

// Define all App types
// Create all App constructors

class AppEvent[A] private[core] (graph: ReplicationGraph)
    extends MockEvent[AppTier, A](graph)

object AppEvent {
  def apply[A](ev: core.Event[A]): AppEvent[A] = empty
  def empty[A]: AppEvent[A]                    = new AppEvent(ReplicationGraph.start)

  implicit class ReplicableEvent[A](appEv: AppEvent[Client => Option[A]]) {
    def toClient(implicit da: Decoder[A], ea: Encoder[A]): ClientEvent[A] = {
      val hokkoBuilder = implicitly[HokkoBuilder[ClientTier]]
      val source       = core.Event.source[A]
      val newGraph     = ReplicationGraph.eventReceiver(appEv.graph, source)
      hokkoBuilder.event(source, newGraph)
    }
  }
}

class AppBehavior[A] private[core] (graph: ReplicationGraph)
    extends MockBehavior[AppTier, A](graph)

class AppDiscreteBehavior[A] private[core] (
    graph: ReplicationGraph,
    initial: A
) extends MockDiscreteBehavior[AppTier, A](graph, initial)

class AppIncBehavior[A, DeltaA] private[core] (
    graph: ReplicationGraph,
    accumulator: (A, DeltaA) => A,
    initial: A
) extends MockIncBehavior[AppTier, A, DeltaA](graph, accumulator, initial) {
  def client: Client = ???
}

object AppIncBehavior {
  implicit class ReplicableIBehavior[A, DeltaA](
      appBeh: AppIncBehavior[Client => A, Client => Option[DeltaA]]) {

    // FIXME: find a good spot for this
    def transform[X, Y](
        f: (Client => X, Client => Option[Y]) => (Client => X)
    ): (X, Y) => X = { (x, y) =>
      val xf      = (_: Client) => x
      val yf      = (_: Client) => (Some(y): Option[Y])
      val resultF = f(xf, yf)
      resultF(null) // this is fine | we want this to blow up if it gets used
    }

    // TODO: Things that should be tested since this was added:
    // 1. behaviors that are actually being reset (server and client side [DONE (somewhat)])
    // 2. behaviors that have initial values

    def toClient(implicit da: Decoder[A],
                 dda: Decoder[DeltaA],
                 ea: Encoder[A],
                 eda: Encoder[DeltaA]): ClientIncBehavior[A, DeltaA] = {
      val hokkoBuilder = implicitly[HokkoBuilder[ClientTier]]

      // FIXME: these should be provided by behaviorReceiver
      val deltas = core.Event.source[DeltaA]
      val resets = core.Event.source[A]

      val newGraph =
        ReplicationGraph.behaviorReceiver(appBeh.graph, deltas, resets)

      val union =
        deltas.unionWith(resets)(Xor.left[DeltaA, A])(Xor.right) { (l, r) =>
          Xor.right(r)
        }

      // FIXME: this is the initial value on clients before the application works,
      // we should do something smart here
      //   1. use the latest value on the server  (- include it in the html or js or something) (+ state is minimally inconsistent)
      //   2. use the initial server values (- state is severely inconsistent) (- add initial values to everything / needed anyway!) [DONE]
      val init: A = appBeh.initial(appBeh.client)

      val transformedAccumulator = transform(appBeh.accumulator)

      val replicatedBehavior = union.fold(init) { (acc, n) =>
        n match {
          case Xor.Left(delta)  => transformedAccumulator(acc, delta)
          case Xor.Right(reset) => reset
        }
      }

      val justDeltas = replicatedBehavior.deltas.collect { xor =>
        xor match {
          case Xor.Left(delta) => Some(delta)
          case _               => None
        }
      }

      val replicatedBehaviorWithoutXor =
        replicatedBehavior.withDeltas(init, justDeltas)

      hokkoBuilder.incrementalBehavior(replicatedBehaviorWithoutXor,
                                       init,
                                       newGraph,
                                       transformedAccumulator)
    }
  }
}

final class AppTier extends MockTier with AppTierLike {
  type T = AppTier
}

object AppBehavior extends MockBehaviorOps[AppTier]
object AppDiscreteBehavior extends MockDiscreteBehaviorOps[AppTier]
