package mtfrp.core

trait AppBehaviorObject {
  def toSession[A](appBehavior: AppBehavior[A]): SessionBehavior[A] = {
    val appClientsToA = AppBehavior.clients.map2(appBehavior) { (clients, a) =>
      clients.map(c => c -> a).toMap
    }
    new SessionBehavior(appClientsToA)
  }

  val clients: AppBehavior[Set[Client]] = AppDBehavior.clients.toBehavior
}
