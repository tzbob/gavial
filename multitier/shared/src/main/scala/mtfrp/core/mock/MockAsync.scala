package mtfrp.core.mock

import cats.effect.IO
import mtfrp.core.AsyncObject

import scala.concurrent.ExecutionContext

abstract class MockAsync[SubT <: MockTier { type T = SubT }: MockBuilder]
    extends AsyncObject[SubT] {
  val builder = implicitly[MockBuilder[SubT]]

  def execute[A](ev: SubT#Event[IO[A]])(
      implicit ec: ExecutionContext): SubT#Event[A] =
    builder.event(ev.graph)
}
