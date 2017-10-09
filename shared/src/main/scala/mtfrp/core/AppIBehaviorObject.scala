package mtfrp.core

trait AppIBehaviorObject {
  def toSession[A, DeltaA](
      appBehavior: AppIBehavior[A, DeltaA]): SessionIBehavior[A, DeltaA] = {
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
    new SessionIBehavior(appBehaviorBroadcast)
  }

  val clients: AppIBehavior[Set[Client], ClientChange] =
    AppEvent.clientChanges.fold(Set.empty[Client]) { (set, change) =>
      change match {
        case Connected(c)    => set + c
        case Disconnected(c) => set - c
      }
    }
}
