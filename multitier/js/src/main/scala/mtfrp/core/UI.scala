package mtfrp.core

import scalatags.hokko.Builder

import scala.scalajs.js

object UI {
  val html = scalatags.Hokko
  type HTML = html.all.Frag

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
