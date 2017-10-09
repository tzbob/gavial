package mtfrp.core

import cats.data.Ior
import io.circe.{Decoder, Encoder}

import scala.collection.immutable.Map

class SessionIBehavior[A, DeltaA] private[core](
    private[core] val underlying: AppIBehavior[Client => A,
                                                 Client => Option[DeltaA]]
) extends IBehavior[SessionTier, A, DeltaA] {
  private[core] def accumulator: (A, DeltaA) => A =
    IBehavior.transformToNormal(underlying.accumulator)

  def changes: SessionTier#Event[A] = {
    val optChanges = underlying.changes.map { cf => c: Client =>
      Some(cf(c)): Option[A]
    }
    new SessionEvent(optChanges)
  }

  def deltas: SessionTier#Event[DeltaA] = new SessionEvent(underlying.deltas)

  def map[B, DeltaB](fa: A => B)(fb: DeltaA => DeltaB)(
      accumulator: (B, DeltaB) => B)
    : SessionTier#IBehavior[B, DeltaB] = {

    val newUnderlying: AppIBehavior[Client => B, Client => Option[DeltaB]] =
      underlying.map { (cf: (Client => A)) => c: Client =>
        fa(cf(c))
      } { (cf: (Client => Option[DeltaA])) => c: Client =>
        cf(c).map(fb)
      } {
        (accF: (Client => B), newF: (Client => Option[DeltaB])) => c: Client =>
          newF(c) match {
            case Some(newDelta) => accumulator(accF(c), newDelta)
            case None           => accF(c)
          }
      }

    new SessionIBehavior(newUnderlying)
  }

  def map2[B, DeltaB, C, DeltaC](b: SessionTier#IBehavior[B, DeltaB])(
      valueFun: (A, B) => C)(
      deltaFun: (A, B, Ior[DeltaA, DeltaB]) => Option[DeltaC])(
      foldFun: (C, DeltaC) => C): SessionTier#IBehavior[C, DeltaC] = {
    val newUnderlying: AppIBehavior[Client => C, Client => Option[DeltaC]] =
      underlying.map2(b.underlying) {
        (cfA: (Client => A), cfB: (Client => B)) => c: Client =>
          valueFun(cfA(c), cfB(c))
      } { (cfA: Client => A, cfB: Client => B, ior: Ior[Client => Option[DeltaA], Client => Option[DeltaB]]) =>
        val res: (Client) => Option[DeltaC] = ior match {
          case Ior.Left(cfDA) =>
            c: Client =>
              cfDA(c).fold(Option.empty[DeltaC])(da =>
                deltaFun(cfA(c), cfB(c), Ior.Left(da)))
          case Ior.Right(cfDB) =>
            c: Client =>
              cfDB(c).fold(Option.empty[DeltaC])(db =>
                deltaFun(cfA(c), cfB(c), Ior.Right(db)))
          case Ior.Both(cfDA, cfDB) =>
            c: Client =>
              (cfDA(c), cfDB(c)) match {
                case (Some(da), Some(db)) =>
                  deltaFun(cfA(c), cfB(c), Ior.Both(da, db))
                case (Some(da), _) =>
                  deltaFun(cfA(c), cfB(c), Ior.Left(da))
                case (_, Some(db)) =>
                  deltaFun(cfA(c), cfB(c), Ior.Right(db))
                case (None, None) => None
              }
        }
        // TODO: this always fires an update even if there wasn't actually any
        // work to be done for any clients
        Option(res)
      } { (cfC: Client => C, cfDC: Client => Option[DeltaC]) => c: Client =>
        cfDC(c).fold(cfC(c))(dc => foldFun(cfC(c), dc))
      }

    new SessionIBehavior(newUnderlying)
  }

  def snapshotWith[B, C](ev: SessionTier#Event[B])(
      f: (A, B) => C): SessionTier#Event[C] =
    new SessionEvent(
      underlying.snapshotWith(ev.underlying) {
        (af: Client => A, bf: Client => Option[B]) => c: Client =>
          bf(c).map { b =>
            f(af(c), b)
          }
      }
    )

  def toDBehavior: SessionTier#DBehavior[A] =
    new SessionDBehavior(underlying.toDBehavior)
}

object SessionIBehavior extends IBehaviorObject[SessionTier] {
  override def constant[A, B](x: A): SessionIBehavior[A, B] = {
    val src = AppEvent.empty[Client => Option[B]]
    val inc: AppIBehavior[Client => A, Client => Option[B]] =
      src.fold((_: Client) => x) { (f, _) =>
        f
      }
    new SessionIBehavior(inc)
  }

  def toApp[A, DeltaA](sessionB: SessionIBehavior[A, DeltaA])
    : AppIBehavior[Map[Client, A],
                     (Map[Client, DeltaA], Option[ClientChange])] = {
    // FIXME Relying on map.withDefault AGAIN (switch to initial values?)
    sessionB.underlying.map2(AppIBehavior.clients) { (cfA: Client => A, clients: Set[Client]) =>
      clients.map(c => c -> cfA(c)).toMap.withDefault(cfA)
    } { (cfA: Client => A, clients: Set[Client], ior: Ior[Client => Option[DeltaA], ClientChange]) =>
      ior match {
        case Ior.Left(cfDA) =>
          val deltas: Map[Client, DeltaA] = clients.collect {
            case c if cfDA(c).isDefined => c -> cfDA(c).get
          }.toMap
          Some(deltas -> None)
        case Ior.Right(cc) =>
          Some(Map.empty[Client, DeltaA] -> Some(cc))
        case Ior.Both(cfDA, cc) =>
          val deltas: Map[Client, DeltaA] = clients.collect {
            case c if cfDA(c).isDefined => c -> cfDA(c).get
          }.toMap
          Some(deltas -> Some(cc))
      }
    } {
      case (aMap, (daMap, ccOpt)) =>
        import cats._
        import cats.data._
        import cats.implicits._

        val newAMap = daMap.map2(aMap) { (da, a) =>
          sessionB.accumulator(a, da)
        }
        ccOpt match {
          case Some(Disconnected(c)) => newAMap - c
          case Some(Connected(c))    => newAMap + (c -> newAMap(c))
          case _                     => newAMap
        }
    }
  }

  def toClient[A, DeltaA](sessionB: SessionIBehavior[A, DeltaA])(
      implicit dec: Decoder[A],
      decD: Decoder[DeltaA],
      enc: Encoder[A],
      encD: Encoder[DeltaA]): ClientIBehavior[A, DeltaA] =
    AppIBehavior.toClient(sessionB.underlying)
}
