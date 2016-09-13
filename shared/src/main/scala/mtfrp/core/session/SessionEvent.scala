package mtfrp.core.session

import hokko.{core => HC}
import io.circe._
import mtfrp.core._

class SessionEvent[A] private[session] (
    val underlying: AppEvent[Client => Option[A]]
) extends Event[SessionTier, A] {
  private[core] val graph: ReplicationGraph = underlying.graph

  def collect[B](fb: A => Option[B]): SessionTier#Event[B] = {
    val newUnderlying = underlying.collect { clientFun =>
      // TODO: this always fires an update even if there wasn't actually any
      // work to be done for any clients
      Some { c: Client =>
        clientFun(c).flatMap(fb)
      }
    }
    new SessionEvent(newUnderlying)
  }

  def fold[B](initial: B)(
      f: (B, A) => B): SessionTier#IncrementalBehavior[B, A] = {

    val initialF: (Client) => B = (_: Client) => initial
    val newRep = underlying.fold(initialF) {
      (stateFun, newEvent) => c: Client =>
        val state = stateFun(c)
        newEvent(c) match {
          case Some(pulse) => f(state, pulse)
          case None        => state
        }
    }

    new SessionIncrementalBehavior(newRep)
  }

  def unionWith[B, C](b: SessionTier#Event[B])(f1: A => C)(
      f2: B => C)(f3: (A, B) => C): SessionTier#Event[C] = {
    val newRep = underlying.unionWith(b.underlying) { clientFun => c: Client =>
      clientFun(c).map(f1)
    } { clientFun => c: Client =>
      clientFun(c).map(f2)
    } { (aaClientFun, bClientFun) => c: Client =>
      (aaClientFun(c), bClientFun(c)) match {
        case (None, _)           => None
        case (_, None)           => None
        case (Some(aa), Some(b)) => Some(f3(aa, b))
      }
    }

    new SessionEvent(newRep)
  }

  def toClient(implicit dec: Decoder[A], enc: Encoder[A]): ClientEvent[A] =
    underlying.toClient
}

object SessionEvent extends EventObject[SessionTier] {
  override def empty[A]: SessionEvent[A] =
    new SessionEvent(AppEvent.empty[Client => Option[A]])
  override private[core] def apply[A](ev: HC.Event[A]): SessionEvent[A] = {
    val hcEv = ev.map { a => c: Client =>
      Some(a)
    }
    new SessionEvent(AppEvent(hcEv))
  }
}
