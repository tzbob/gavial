package mtfrp.core

trait AppDBehaviorObject {
  def toSession[A](appBehavior: AppDBehavior[A]): SessionDBehavior[A] = {

    val appClientsToA = AppDBehavior.clients.map2(appBehavior) { (clients, a) =>
      clients.map(c => c -> a).toMap
    }
    new SessionDBehavior(appClientsToA, true)
  }

  val clients: AppDBehavior[Set[Client]] = AppIBehavior.clients.toDBehavior
}
