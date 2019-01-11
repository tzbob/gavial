package mtfrp.core
import cats.{CommutativeApplicative, Traverse, UnorderedTraverse}
import cats.effect.IO

import scala.concurrent.ExecutionContext

import cats._
import cats.data._
import cats.implicits._

import cats.effect.implicits._

object SessionAsync extends AsyncObject[SessionTier] {
  def execute[A](ev: SessionEvent[IO[A]])(
      implicit ec: ExecutionContext): SessionEvent[A] = {

    val sequenced = ev.underlying.map { (ios: Map[Client, IO[A]]) =>
      ios.map(_.sequence).toList.sequence.map(_.toMap)
    }

    val result = AppAsync.execute(sequenced)

    new SessionEvent(result, result.graph)
  }
}
