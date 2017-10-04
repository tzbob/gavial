package mtfrp.core

import hokko.core.Engine
import hokko.{core => HC}
import org.scalajs.dom
import slogging.LazyLogging
import snabbdom.VNode

import scala.language.reflectiveCalls
import scala.scalajs.js
import scalatags.hokko.{Builder, DomPatcher}

trait MyMain
    extends js.JSApp
    with FrpMain[Builder, Engine => VNode, Engine => VNode]
    with LazyLogging {
  val html = scalatags.Hokko

  object UI {
    import html.all._

    def read[Result](tag: BaseTagType)(sink: ClientBehaviorSink[Result],
                                       selector: dom.Element => Result): HTML =
      new TagWithSink(tag).read(sink.rep, selector)

    def listen[Result](a: Attr, src: ClientEventSource[Result])(
        f: dom.Event => Result)
      : scalatags.generic.AttrPair[Builder, hokko.core.EventSource[Result]] =
      a.listen(src.rep, f)
  }

  def main(): Unit = {
    val clientId = ClientGenerator.static.id

    val manager =
      new EventManager(ui.graph, Seq(ui.rep.toCBehavior), Seq(ui.rep.changes))

    manager.start(s"/${Names.ws}/$clientId")

    applyHtml(manager.engine, ui.rep)
  }

  def applyHtml(engine: Engine,
                mainUi: HC.DBehavior[HTML],
                onLoading: Boolean = true): Unit = {
    val initialVDom: Option[HTML] =
      engine.askCurrentValues()(mainUi.toCBehavior)

    val domPatcherOpt =
      initialVDom.map(v => new DomPatcher(v.render(engine)))

    domPatcherOpt match {
      case Some(domPatcher) =>
        def onLoad(x: Any) = {
          val el = dom.document.getElementById("mtfrpcontent")

          while (el.hasChildNodes()) el.removeChild(el.lastChild)
          el.appendChild(domPatcher.parent.firstElementChild)
        }
        if (onLoading)
          dom.document.addEventListener("DOMContentLoaded", onLoad _)
        else onLoad(null)

        engine.subscribeForPulses { pulses =>
          val newVDomOpt = pulses(mainUi.changes).map(_.render)
          newVDomOpt.foreach { (newVDom: (Engine) => VNode) =>
            domPatcher.applyNewState(newVDom(engine))
          }
        }
        ()
      case _ =>
        logger.info(s"Could not create a DomPatcher, no value for: $mainUi")
    }
  }
}
