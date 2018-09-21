package mtfrp.core

import cats.data.Ior
import io.circe._
import cats.data._
import cats.implicits._

trait AppIBehaviorObject {
  def broadcast[A, DeltaA](appBeh: AppIBehavior[A, DeltaA])(
      implicit da: Decoder[A],
      dda: Decoder[DeltaA],
      ea: Encoder[A],
      eda: Encoder[DeltaA]): ClientIBehavior[A, DeltaA] = {
    Replicator.toClient(
      appBeh.initial,
      appBeh.accumulator,
      appBeh.toDBehavior.toBehavior.map { a => (_: Client) =>
        a
      },
      appBeh.deltas.map { d => (_: Client) =>
        Some(d)
      }
    )
  }

  def toSession[A, DeltaA](
      appBehavior: AppIBehavior[A, DeltaA]): SessionIBehavior[A, DeltaA] = {
    val appBehaviorBroadcast
      : AppIBehavior[Map[Client, A], (Map[Client, DeltaA], Set[ClientChange])] =
      appBehavior.map2(AppIBehavior.clients) { (a: A, clients: Set[Client]) =>
        clients.map(_ -> a).toMap
      } { (a: A, clients: Set[Client], ior: Ior[DeltaA, ClientChange]) =>
        ior match {
          case Ior.Left(deltaA) =>
            val deltaMap = clients.map(_ -> deltaA).toMap
            if (deltaMap.isEmpty) None
            else Option(deltaMap -> Set.empty[ClientChange])
          case Ior.Right(change) =>
            Option(Map.empty[Client, DeltaA] -> Set(change))
          case Ior.Both(deltaA, cc) =>
            val deltaMap = clients.map(_ -> deltaA).toMap
            Option(deltaMap -> Set(cc))
        }
      }(IBehavior.transformFromNormalToSetClientChangeMap(
        appBehavior.initial,
        appBehavior.accumulator))

    new SessionIBehavior(appBehaviorBroadcast,
                         appBehavior.initial,
                         appBehaviorBroadcast.graph.ws)
  }

  val clients: AppIBehavior[Set[Client], ClientChange] =
    AppEvent.clientChanges.fold(Set.empty[Client]) { (set, change) =>
      change match {
        case Connected(c)    => set + c
        case Disconnected(c) => set - c
      }
    }
}
