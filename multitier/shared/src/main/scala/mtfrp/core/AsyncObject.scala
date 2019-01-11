package mtfrp.core
import cats.effect.IO

import scala.concurrent.ExecutionContext

trait AsyncObject[SubT <: Tier { type T = SubT }] {
  def execute[A](ev: SubT#Event[IO[A]])(
      implicit ec: ExecutionContext): SubT#Event[A]
}
