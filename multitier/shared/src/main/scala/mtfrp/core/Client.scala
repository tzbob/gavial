package mtfrp.core

import java.util.UUID

import io.circe.{KeyDecoder, KeyEncoder}

case class Client(id: UUID)

object Client {
  implicit val clientKeyEncoder = new KeyEncoder[Client] {
    def apply(key: Client): String = key.id.toString
  }

  implicit val clientKeyDecoder = new KeyDecoder[Client] {
    def apply(key: String): Option[Client] =
      KeyDecoder.decodeKeyUUID(key).map(Client.apply)
  }
}
