package mtfrp.core

import mtfrp.core.macros.Dummy
import scalatags.generic.AttrPair
import scalatags.text.Builder

object UI {
  val html = scalatags.Text
  type HTML = html.all.Frag

  import html.all._

  def read[Result](tag: BaseTagType)(sink: Any,
                                     selector: Dummy => Result): HTML = tag

  def listen[Result](a: Attr, src: ClientEventSource[Result])(
      f: Dummy => Result)
    : scalatags.generic.AttrPair[Builder, hokko.core.EventSource[Result]] =
    AttrPair(a, null, new AttrValue[hokko.core.EventSource[Result]] {
      def apply(t: Builder, a: Attr, v: hokko.core.EventSource[Result]) = t
    })
}
