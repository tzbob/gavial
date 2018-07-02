package mtfrp.core

trait AppBehaviorObject {
  def toSession[A](appBehavior: AppBehavior[A]): SessionBehavior[A] = {
    val appClientsToA = AppBehavior.clients.map2(appBehavior) { (clients, a) =>
      clients.map(c => c -> a).toMap
    }
    new SessionBehavior(appClientsToA, appClientsToA.graph.ws)
  }

  val clients: AppBehavior[Set[Client]] = AppDBehavior.clients.toBehavior
}
