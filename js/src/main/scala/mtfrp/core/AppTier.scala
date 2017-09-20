package mtfrp
package core

import hokko.core.{IBehavior, Event => HEvent}
import io.circe._
import mtfrp.core.impl.HokkoBuilder
import mtfrp.core.mock._

// Define all App types
// Create all App constructors

class AppEvent[A] private[core] (graph: ReplicationGraph)
    extends MockEvent[AppTier, A](graph)

object AppEvent extends MockEventObject {
  def toClient[A](appEv: AppEvent[Client => Option[A]])(
      implicit da: Decoder[A],
      ea: Encoder[A]): ClientEvent[A] = {
    val hokkoBuilder = implicitly[HokkoBuilder[ClientTier]]
    val newGraph     = ReplicationGraphClient.ReceiverEvent(appEv.graph)
    hokkoBuilder.event(newGraph.source, newGraph)
  }

  implicit class ReplicableEvent[A](appEv: AppEvent[Client => Option[A]]) {
    def toClient(implicit da: Decoder[A], ea: Encoder[A]): ClientEvent[A] =
      AppEvent.toClient(appEv)
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

import slogging.LazyLogging

object AppIncBehavior
    extends MockIncrementalBehaviorObject[AppTier]
    with LazyLogging {

  def toClient[A, DeltaA](
      appBeh: AppIncBehavior[Client => A, Client => Option[DeltaA]])(
      implicit da: Decoder[A],
      dda: Decoder[DeltaA],
      ea: Encoder[A],
      // TODO: This result type is incorrect
      eda: Encoder[DeltaA]): ClientIncBehavior[A, Either[DeltaA, A]] = {

    val hokkoBuilder = implicitly[HokkoBuilder[ClientTier]]

    val newGraph =
      ReplicationGraphClient.ReceiverBehavior[A, DeltaA](appBeh.graph)

    val deltas = newGraph.deltas.source
    val resets = newGraph.resets

    // explicit types needed: SI-9772

    val lefts: HEvent[Either[DeltaA, A]] = deltas.map(Left.apply[DeltaA, A])
    val rights: HEvent[Either[DeltaA, A]] =
      resets.map(Right.apply[DeltaA, A])
    val union: HEvent[Either[DeltaA, A]] = lefts.unionWith(rights) {
      case (_, Right(r)) => Right(r)
      case (Left(l), _)  => Left(l)
      case (_, _) =>
        logger.error("Deltas and Resets mixed up.")
        sys.error("Deltas and Resets mixed up.")
    }

    // TODO: Improve with an initial value reader/injector
    /*
    This is correct, the actual initial values are sent as a
    reset request, the initial values of behaviors are only used as
    an asap-initialisation mechanism.
     */
    val init: A = appBeh.initial(ClientGenerator.static)

    val transformedAccumulator =
      IncrementalBehavior.transformToNormal(appBeh.accumulator)

    val replicatedBehavior: IBehavior[A, Either[DeltaA, A]] =
      union.fold(init) { (acc, n) =>
        n match {
          case Left(delta)  => transformedAccumulator(acc, delta)
          case Right(reset) => reset
        }
      }

    val f: (A, Either[DeltaA, A]) => A = (whole, either) => {
      either match {
        case Left(deltaA) =>
          transformedAccumulator(whole, deltaA)
        case Right(a) =>
          whole
      }
    }

    hokkoBuilder.incrementalBehavior(replicatedBehavior, init, newGraph, f)
  }

  implicit class ReplicableIBehavior[A, DeltaA](
      appBeh: AppIncBehavior[Client => A, Client => Option[DeltaA]]) {
    def toClient(
        implicit da: Decoder[A],
        dda: Decoder[DeltaA],
        ea: Encoder[A],
        eda: Encoder[DeltaA]): ClientIncBehavior[A, Either[DeltaA, A]] =
      AppIncBehavior.toClient(appBeh)
  }

}

final class AppTier extends MockTier with AppTierLike
