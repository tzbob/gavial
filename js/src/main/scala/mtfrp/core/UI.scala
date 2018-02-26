package mtfrp.core

import scala.scalajs.js
import scalatags.hokko.Builder

object UI {
  val html = scalatags.Hokko
  type HTML = html.Tag

  import html.all._

  def read[Result](tag: BaseTagType)(sink: ClientBehaviorSink[Result],
                                     selector: js.Dynamic => Result): HTML =
    new TagWithSink(tag)
      .read(sink.rep, el => selector(el.asInstanceOf[js.Dynamic]))

  def listen[Result](a: Attr, src: ClientEventSource[Result])(
      f: js.Dynamic => Result)
    : scalatags.generic.AttrPair[Builder, hokko.core.EventSource[Result]] =
    a.listen(src.rep, el => f(el.asInstanceOf[js.Dynamic]))
}
