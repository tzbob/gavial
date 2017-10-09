package mtfrp.core

trait AppDiscreteBehaviorObject {
  def toSession[A](
      appBehavior: AppDiscreteBehavior[A]): SessionDiscreteBehavior[A] =
    new SessionDiscreteBehavior(appBehavior.map { v => _: Client =>
      v
    })

  val clients: AppDiscreteBehavior[Set[Client]] =
    AppIncBehavior.clients.toDiscreteBehavior
}
