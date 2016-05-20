package mtfrp.core

trait ClientChange {
  val client: Client
}

case class Connected(client: Client) extends ClientChange

case class Disconnected(client: Client) extends ClientChange
