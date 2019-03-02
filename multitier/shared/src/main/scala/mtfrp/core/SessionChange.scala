package mtfrp.core

private[core] sealed trait SessionChange[+A] {
  val clientChange: ClientChange
  def map[B](f: A => B): SessionChange[B]
}

private[core] object SessionChange {
  def fromClientChange[A](c: ClientChange, current: A): SessionChange[A] =
    c match {
      case conn @ Connected(_)  => Connect(conn, current)
      case dc @ Disconnected(_) => Disconnect(dc)
    }

  def applyToDeltaMap[DA, A](
      map: Map[Client, DA],
      change: Option[SessionChange[A]]): Map[Client, DA] = {
    change.fold(map) {
      case SessionChange.Disconnect(Disconnected(client)) =>
        map - client
      case _ => map
    }
  }

  def applyToStateMap[A](map: Map[Client, A],
                         change: Option[SessionChange[A]]): Map[Client, A] = {
    change.fold(map) {
      case Connect(Connected(client), current) =>
        map.updated(client, map.getOrElse(client, current))
      case Disconnect(Disconnected(client)) => map - client
    }
  }

  case class Connect[+A](c: Connected, current: A) extends SessionChange[A] {
    def map[B](f: A => B): SessionChange[B] = Connect(c, f(current))
    val clientChange: ClientChange          = c
  }
  case class Disconnect(d: Disconnected) extends SessionChange[Nothing] {
    def map[B](f: Nothing => B): SessionChange[B] = Disconnect(d)
    val clientChange: ClientChange                = d
  }
}
