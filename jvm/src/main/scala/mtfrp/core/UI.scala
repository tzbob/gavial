package mtfrp.core

import scala.language.dynamics
import scalatags.generic.{Attr, AttrPair}
import scalatags.text.Builder

object UI {
  val html = scalatags.Text
  type HTML = html.Tag

  import html.all._

  trait Dummy extends Dynamic {
    def selectDynamic(methodName: String): Dummy               = ???
    def applyDynamic(methodName: String)(argument: Any): Dummy = ???
  }

  def read[Result](tag: BaseTagType)(sink: Any,
                                     selector: Dummy => Result): HTML = tag

  def listen[Result](a: Attr, src: ClientEventSource[Result])(f: Any => Result)
    : scalatags.generic.AttrPair[Builder, hokko.core.EventSource[Result]] =
    AttrPair(a, null, new AttrValue[hokko.core.EventSource[Result]] {
      def apply(t: Builder, a: Attr, v: hokko.core.EventSource[Result]) = t
    })
}
