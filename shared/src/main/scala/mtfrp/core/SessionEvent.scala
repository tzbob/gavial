package mtfrp
package core

// TODO: Implement Session Events using App Events and add toClient support
// look at SessionFRPLib
class SessionEvent[A] private[core] (
    val underlying: AppEvent[Client => Option[A]]
) extends Event[SessionTier, A] {
  private[core] val graph: ReplicationGraph = underlying.graph

  def collect[B, AA >: A](fb: A => Option[B]): SessionTier#Event[B] = {
    val newUnderlying = underlying.collect { clientFun =>
      // TODO: this always fires an update even if there wasn't actually any
      // work to be done for any clients
      Some { c: Client =>
        clientFun(c).flatMap(fb)
      }
    }
    new SessionEvent(newUnderlying)
  }

  def dropIf[B](f: A => Boolean): SessionTier#Event[A] = ???
  def fold[B, AA >: A](initial: B)(
      f: (B, AA) => B): SessionTier#IncrementalBehavior[B, AA] = ???
  def hold[AA >: A](initial: AA): SessionTier#DiscreteBehavior[AA] = ???
  def map[B](f: A => B): SessionTier#Event[B] = ???
  def mergeWith[AA >: A](
      events: SessionTier#Event[AA]*): SessionTier#Event[Seq[AA]] = ???
  def unionLeft[AA >: A](other: SessionTier#Event[AA]): SessionTier#Event[AA] =
    ???
  def unionRight[AA >: A](
      other: SessionTier#Event[AA]): SessionTier#Event[AA] = ???
  def unionWith[B, C, AA >: A](b: SessionTier#Event[B])(f1: AA => C)(
      f2: B => C)(f3: (AA, B) => C): SessionTier#Event[C] = ???

  // def toClient(implicit dec: Decoder[A], enc: Encoder[A]): ClientEvent[A] =
  //   underlying.replicateToClient
}
