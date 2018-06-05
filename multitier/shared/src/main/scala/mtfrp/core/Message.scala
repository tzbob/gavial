package mtfrp
package core

import io.circe._
import io.circe.syntax._

case class Message(id: Int, payload: Json)

object Message {
  def fromPayload[A: Encoder](id: Int)(payload: A): Message =
    Message(id, payload.asJson)
}
