package mtfrp.core

import org.scalajs.dom

import scala.language.reflectiveCalls
import scalatags.hokko.Builder

object UI {
  val html = scalatags.Hokko
  type HTML = html.Tag

  import html.all._

  def read[Result](tag: BaseTagType)(sink: ClientBehaviorSink[Result],
                                     selector: dom.Element => Result): HTML =
    new TagWithSink(tag).read(sink.rep, selector)

  def listen[Result](a: Attr, src: ClientEventSource[Result])(
      f: dom.Event => Result)
    : scalatags.generic.AttrPair[Builder, hokko.core.EventSource[Result]] =
    a.listen(src.rep, f)
}
