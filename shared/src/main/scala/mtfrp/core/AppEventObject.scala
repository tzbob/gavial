package mtfrp.core

trait AppEventObject {
  def toSession[A](appEvent: AppEvent[A]): SessionEvent[A] = {
    val appEvBroadcast = appEvent.map { v => (_: Client) =>
      Some(v): Option[A]
    }
    new SessionEvent(appEvBroadcast)
  }
}
