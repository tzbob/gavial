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
) extends MockIncBehavior[AppTier, A, DeltaA](graph, accumulator, initial)

object AppIncBehavior {
  implicit class ReplicableIBehavior[A, DeltaA](
      appBeh: AppIncBehavior[Client => A, Client => Option[DeltaA]]) {

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
      //   1. use the latest value on the server [TODO]
      //   2. use the initial server values [DONE]
      val init: A = appBeh.initial(ClientGenerator.static)

      val transformedAccumulator =
        IncrementalBehavior.transform(appBeh.accumulator)

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
