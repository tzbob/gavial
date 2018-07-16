package mtfrp.core

import cats.data.Ior
import io.circe.{Decoder, Encoder}

import scala.collection.immutable.Map
import cats.data._
import cats.implicits._
import slogging.LazyLogging

class SessionIBehavior[A, DeltaA] private[core] (
    private[core] val underlying: AppIBehavior[Map[Client, A],
                                               Map[Client, DeltaA]],
    private[core] val initial: A,
    graphByName: GraphState
) extends IBehavior[SessionTier, A, DeltaA] {
  private[core] val graph = graphByName

  private[core] def accumulator: (A, DeltaA) => A =
    IBehavior.transformFromMap(underlying.accumulator)

  def changes: SessionTier#Event[A] =
    new SessionEvent(underlying.changes, this.graph)

  def deltas: SessionTier#Event[DeltaA] =
    new SessionEvent(underlying.deltas, this.graph)

  def map[B, DeltaB](fa: A => B)(fb: DeltaA => DeltaB)(
      accumulator: (B, DeltaB) => B): SessionTier#IBehavior[B, DeltaB] = {

    val newUnderlying: AppIBehavior[Map[Client, B], Map[Client, DeltaB]] =
      underlying.map { (aMap: Map[Client, A]) =>
        aMap.mapValues(fa)
      } { (deltaMap: Map[Client, DeltaA]) =>
        deltaMap.mapValues(fb)
      } { (accF: Map[Client, B], newF: Map[Client, DeltaB]) =>
        val accumulated = accF.map2(newF)(accumulator)
        accF ++ accumulated
      }

    new SessionIBehavior(newUnderlying, fa(initial), this.graph)
  }

  def map2[B, DeltaB, C, DeltaC](b: SessionTier#IBehavior[B, DeltaB])(
      valueFun: (A, B) => C)(
      deltaFun: (A, B, Ior[DeltaA, DeltaB]) => Option[DeltaC])(
      foldFun: (C, DeltaC) => C): SessionTier#IBehavior[C, DeltaC] = {
    val newUnderlying: AppIBehavior[Map[Client, C], Map[Client, DeltaC]] =
      underlying.map2(b.underlying) {
        (aMap: Map[Client, A], bMap: Map[Client, B]) =>
          aMap.map2(bMap)(valueFun)
      } {
        (aMap: Map[Client, A],
         bMap: Map[Client, B],
         ior: Ior[Map[Client, DeltaA], Map[Client, DeltaB]]) =>
          val iorMap = ior match {
            case Ior.Left(deltaAMap) =>
              deltaAMap.mapValues(Ior.left)

            case Ior.Right(deltaBMap) =>
              deltaBMap.mapValues(Ior.right)

            case Ior.Both(deltaAMap, deltaBMap) =>
              val iorLeft  = deltaAMap.mapValues(Ior.left)
              val iorRight = deltaBMap.mapValues(Ior.right)
              val iorBoth  = deltaAMap.map2(deltaBMap)(Ior.both)

              iorLeft ++ iorRight ++ iorBoth
          }

          val deltaMappedMap = (aMap, bMap, iorMap).mapN(deltaFun)
          val filteredMap    = deltaMappedMap.filter(_._2.isDefined)

          if (filteredMap.isEmpty) None
          else Option(filteredMap.mapValues(_.get))
      } { (cMap: Map[Client, C], deltaCMap: Map[Client, DeltaC]) =>
        cMap.map2(deltaCMap)(foldFun)
      }

    new SessionIBehavior(newUnderlying,
                         valueFun(this.initial, b.initial),
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
      // FIXME, this gives an update that should be invisible to session
      // behaviors
      Map.empty[Client, B]
    } { (cMap, _) =>
      cMap
    }
    new SessionIBehavior(underlying, x, underlying.graph.ws)
  }

  def toApp[A, DeltaA](sessionB: SessionIBehavior[A, DeltaA])
    : AppIBehavior[Map[Client, A],
                   (Map[Client, DeltaA], Option[ClientChange])] = {
    sessionB.underlying.map2(AppIBehavior.clients) {
      (aMap: Map[Client, A], clients: Set[Client]) =>
        val toDelete = aMap.keySet -- clients
        aMap -- toDelete
    } {
      (_: Map[Client, A],
       _: Set[Client],
       ior: Ior[Map[Client, DeltaA], ClientChange]) =>
        ior match {
          case Ior.Left(iorMap) =>
            if (iorMap.isEmpty) None else Option(iorMap -> None)
          case Ior.Right(cc) =>
            Option(Map.empty[Client, DeltaA] -> Option(cc))
          case Ior.Both(iorMap, cc) =>
            Option(iorMap -> Option(cc))
        }
    } {
      case (aMap: Map[Client, A],
            (daMap: Map[Client, DeltaA], ccOpt: Option[ClientChange])) =>
        val newAMap = aMap.map2(daMap)(sessionB.accumulator)

        ccOpt match {
          case Some(Disconnected(c)) => newAMap - c
          case Some(Connected(c))    => newAMap + (c -> sessionB.initial)
          case _                     => newAMap
        }
    }
  }

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
      sessionB.underlying.deltas.map(_.get _)

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
