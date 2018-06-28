package mtfrp.core

trait AppEventObject {
  def toSession[A](appEvent: AppEvent[A]): SessionEvent[A] = {
    val appEvBroadcast = AppBehavior.clients.snapshotWith(appEvent) {
      (clients, event) =>
        clients.map(c => c -> event).toMap
    }
    new SessionEvent(appEvBroadcast,
                     appEvBroadcast.graph.copy(requiresWebSockets = true))
  }
}
