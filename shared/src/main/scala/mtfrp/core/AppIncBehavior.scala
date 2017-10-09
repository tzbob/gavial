package mtfrp.core

trait AppIncBehaviorObject {
  def toSession[A, DeltaA](
      appBehavior: AppIncBehavior[A, DeltaA]): SessionIncBehavior[A, DeltaA] = {
    val appBehaviorBroadcast = appBehavior.map { v => _: Client =>
      v
    } { deltaA => _: Client =>
      Some(deltaA): Option[DeltaA]
    } { (cfA, cfDA) => (c: Client) =>
      cfDA(c) match {
        case Some(da) => appBehavior.accumulator(cfA(c), da)
        case None     => cfA(c)
      }
    }
    new SessionIncBehavior(appBehaviorBroadcast)
  }

  val clients: AppIncBehavior[Set[Client], ClientChange] =
    AppEvent.clientChanges.fold(Set.empty[Client]) { (set, change) =>
      change match {
        case Connected(c)    => set + c
        case Disconnected(c) => set - c
      }
    }
}
