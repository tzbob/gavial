package mtfrp.core

trait AppDBehaviorObject {
  def toSession[A](
      appBehavior: AppDBehavior[A]): SessionDBehavior[A] =
    new SessionDBehavior(appBehavior.map { v =>_: Client =>
      v
    })

  val clients: AppDBehavior[Set[Client]] =
    AppIBehavior.clients.toDBehavior
}
