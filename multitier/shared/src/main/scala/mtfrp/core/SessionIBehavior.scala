package mtfrp.core

import cats.data.Ior
import io.circe.{Decoder, Encoder}

import scala.collection.immutable.Map
import cats.data._
import cats.implicits._
import slogging.LazyLogging

class SessionIBehavior[A, DeltaA] private[core] (
    private[core] val underlying: AppIBehavior[Map[Client, A],
                                               (Map[Client, DeltaA],
                                                Set[ClientChange])],
    private[core] val initial: A,
    graphByName: GraphState
) extends IBehavior[SessionTier, A, DeltaA] {
  private[core] val graph = graphByName

  private[core] def accumulator: (A, DeltaA) => A =
    (acc, delta) =>
      underlying
        .accumulator(Map(ClientGenerator.static -> acc),
                     Map(ClientGenerator.static -> delta) -> Set.empty)
        .apply(ClientGenerator.static)

  def changes: SessionTier#Event[A] =
    new SessionEvent(underlying.changes, this.graph)

  def deltas: SessionTier#Event[DeltaA] =
    new SessionEvent(
      underlying.deltas.map {
        (change: (Map[Client, DeltaA], Set[ClientChange])) =>
          val (map, changes) = change
          map -- changes.collect { case Disconnected(c) => c }
      },
      this.graph
    )

  def map[B, DeltaB](fa: A => B)(fb: DeltaA => DeltaB)(
      accumulator: (B, DeltaB) => B): SessionTier#IBehavior[B, DeltaB] = {

    val newUnderlying
      : AppIBehavior[Map[Client, B], (Map[Client, DeltaB], Set[ClientChange])] =
      underlying.map { (aMap: Map[Client, A]) =>
        aMap.mapValues(fa)
      } { (change: (Map[Client, DeltaA], Set[ClientChange])) =>
        change.swap.map(_.mapValues(fb)).swap
      }(
        IBehavior.transformFromNormalToSetClientChangeMap(fa(this.initial),
                                                          accumulator)
      )

    new SessionIBehavior(newUnderlying, fa(initial), this.graph)
  }

  def map2[B, DeltaB, C, DeltaC](b: SessionTier#IBehavior[B, DeltaB])(
      valueFun: (A, B) => C)(
      deltaFun: (A, B, Ior[DeltaA, DeltaB]) => Option[DeltaC])(
      foldFun: (C, DeltaC) => C): SessionTier#IBehavior[C, DeltaC] = {
    val v = valueFun(this.initial, b.initial)

    val newUnderlying
      : AppIBehavior[Map[Client, C], (Map[Client, DeltaC], Set[ClientChange])] =
      underlying.map2(b.underlying) {
        (aMap: Map[Client, A], bMap: Map[Client, B]) =>
          aMap.map2(bMap)(valueFun)
      } {
        (aMap: Map[Client, A],
         bMap: Map[Client, B],
         ior: Ior[(Map[Client, DeltaA], Set[ClientChange]),
                  (Map[Client, DeltaB], Set[ClientChange])]) =>
          val (iorMap, changes) = ior match {
            case Ior.Left((deltaAMap, changes)) =>
              deltaAMap.mapValues(Ior.left) -> changes

            case Ior.Right((deltaBMap, changes)) =>
              deltaBMap.mapValues(Ior.right) -> changes

            case Ior.Both((deltaAMap, changes1), (deltaBMap, changes2)) =>
              val iorLeft  = deltaAMap.mapValues(Ior.left)
              val iorRight = deltaBMap.mapValues(Ior.right)
              val iorBoth  = deltaAMap.map2(deltaBMap)(Ior.both)

              (iorLeft ++ iorRight ++ iorBoth) -> (changes1 ++ changes2)
          }

          val result: (Map[Client, DeltaC], Set[ClientChange]) =
            (aMap, bMap, iorMap)
              .mapN(deltaFun)
              .collect { case (k, Some(v)) => k -> v } -> changes

          if (result._1.isEmpty && result._2.isEmpty) None
          else Option(result)
      }(IBehavior.transformFromNormalToSetClientChangeMap(v, foldFun))

    new SessionIBehavior(newUnderlying,
                         v,
                         GraphState.any.combine(this.graph, b.graph))
  }

  def snapshotWith[B, C](ev: SessionTier#Event[B])(
      f: (A, B) => C): SessionTier#Event[C] =
    new SessionEvent(underlying.snapshotWith(ev.underlying) { (aMap, bMap) =>
      aMap.map2(bMap)(f)
    }, ev.graph.mergeGraphAndEffect(this.graph))

  def toDBehavior: SessionTier#DBehavior[A] =
    new SessionDBehavior(underlying.toDBehavior, this.initial, this.graph)
}

object SessionIBehavior extends IBehaviorObject[SessionTier] with LazyLogging {
  override def constant[A, B](x: A): SessionIBehavior[A, B] = {
    val underlying = AppIBehavior.clients.map { clients =>
      clients.map(_ -> x).toMap
    } { clientChange =>
      (Map.empty[Client, B], Set(clientChange))
    } { (cMap, _) =>
      cMap
    }
    new SessionIBehavior(underlying, x, underlying.graph.ws)
  }

  def toApp[A, DeltaA](sessionB: SessionIBehavior[A, DeltaA])
    : AppIBehavior[Map[Client, A], (Map[Client, DeltaA], Set[ClientChange])] =
    sessionB.underlying

  def toClient[A, DeltaA](sessionB: SessionIBehavior[A, DeltaA])(
      implicit dec: Decoder[A],
      decD: Decoder[DeltaA],
      enc: Encoder[A],
      encD: Encoder[DeltaA]): ClientIBehavior[A, DeltaA] = {
    val state: AppBehavior[Client => A] =
      sessionB.toDBehavior.toBehavior.underlying
        .map { (map: Map[Client, A]) => (c: Client) =>
          map.get(c).getOrElse(sessionB.initial)
        }
    val deltas: AppEvent[Client => Option[DeltaA]] =
      sessionB.deltas.underlying.map(_.get _)

    Replicator.toClient(sessionB.initial, sessionB.accumulator, state, deltas)
  }

  def accumulatorFunWithInitial[A, DeltaA](
      state: Map[Client, A],
      newEvent: Map[Client, DeltaA],
      init: A)(f: (A, DeltaA) => A): Map[Client, A] = {
    newEvent.map {
      case (client, a) =>
        val start = state.getOrElse(client, init)
        client -> f(start, a)
    }
  }
}
