package mtfrp
package core

import cats.data.Xor
import io.circe._

// Define all App types
// Create all App constructors

class AppEvent[A] private[core] (graph: ReplicationGraph)
    extends MockEvent[AppTier, A](graph)

object AppEvent extends MockEventObject {
  implicit class ReplicableEvent[A](appEv: AppEvent[Client => Option[A]]) {
    def toClient(implicit da: Decoder[A], ea: Encoder[A]): ClientEvent[A] = {
      val hokkoBuilder = implicitly[HokkoBuilder[ClientTier]]
      val newGraph     = ReplicationGraphClient.ReceiverEvent(appEv.graph)
      hokkoBuilder.event(newGraph.source.toEvent, newGraph)
    }
  }
}

class AppBehavior[A] private[core] (graph: ReplicationGraph)
    extends MockBehavior[AppTier, A](graph)

object AppBehavior extends MockBehaviorObject[AppTier]

class AppDiscreteBehavior[A] private[core] (
    graph: ReplicationGraph,
    initial: A
) extends MockDiscreteBehavior[AppTier, A](graph, initial)

object AppDiscreteBehavior extends MockDiscreteBehaviorObject[AppTier]

class AppIncBehavior[A, DeltaA] private[core] (
    graph: ReplicationGraph,
    accumulator: (A, DeltaA) => A,
    initial: A
) extends MockIncBehavior[AppTier, A, DeltaA](graph, accumulator, initial)

object AppIncBehavior extends MockIncrementalBehaviorObject[AppTier] {
  implicit class ReplicableIBehavior[A, DeltaA](
      appBeh: AppIncBehavior[Client => A, Client => Option[DeltaA]]) {

    def toClient(implicit da: Decoder[A],
                 dda: Decoder[DeltaA],
                 ea: Encoder[A],
                 eda: Encoder[DeltaA]): ClientIncBehavior[A, DeltaA] = {
      val hokkoBuilder = implicitly[HokkoBuilder[ClientTier]]

      val newGraph =
        ReplicationGraphClient.ReceiverBehavior(appBeh.graph)

      val deltas = newGraph.deltas.toEvent
      val resets = newGraph.resets.toEvent
      val union = // explicit types needed: SI-9772
        deltas.unionWith(resets)(Xor.left[DeltaA, A])(Xor.right[DeltaA, A]) { (_: DeltaA, r: A) =>
          Xor.right[DeltaA, A](r)
        }

      // FIXME: this is the initial value on clients before the application works,
      // we should do something smart here
      //   1. use the latest value on the server [TODO]]
      //   2. use the initial server values [DONE]
      val init: A = appBeh.initial(ClientGenerator.static)

      val transformedAccumulator =
        IncrementalBehavior.transformToNormal(appBeh.accumulator)

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

final class AppTier extends MockTier with AppTierLike
