package mtfrp.core.impl

import cats.effect.IO
import hokko.core
import mtfrp.core.AsyncObject

import scala.concurrent.ExecutionContext

abstract class HokkoAsync[SubT <: HokkoTier { type T = SubT }: HokkoBuilder]
    extends AsyncObject[SubT] {
  val builder = implicitly[HokkoBuilder[SubT]]
  def execute[A](ev: SubT#Event[IO[A]])(
      implicit ec: ExecutionContext): SubT#Event[A] =
    builder.event(core.Async.execute(ev.rep), ev.graph)
}
