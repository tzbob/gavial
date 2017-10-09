package mtfrp.core

trait AppBehaviorObject {
  def toSession[A](appBehavior: AppBehavior[A]): SessionBehavior[A] =
    new SessionBehavior(appBehavior.map { v => _: Client =>
      v
    })

  val clients: AppBehavior[Set[Client]] = AppDBehavior.clients.toBehavior
}
