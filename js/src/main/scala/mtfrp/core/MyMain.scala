package mtfrp.core

import hokko.core.Engine
import hokko.{core => HC}
import org.scalajs.dom
import slogging.LazyLogging

import scala.scalajs.js
import scalatags.vdom.Builder
import scalatags.vdom.raw.VirtualDom.VTreeChild

trait MyMain
    extends js.JSApp
    with FrpMain[Builder, VTreeChild, VTreeChild]
    with LazyLogging {
  val html = scalatags.VDom

  def main(): Unit = {
    val clientId = ClientGenerator.static.id

    val manager = new EventManager(ui.graph, Seq(ui.rep), Seq(ui.rep.changes))

    manager.startReceiving(s"/${Names.toClientUpdates}/$clientId")
    manager.startSending(s"/${Names.toServerUpdates}/$clientId")

    applyHtml(manager.engine, ui.rep)
  }

  def applyHtml(engine: Engine, mainUi: HC.DiscreteBehavior[HTML]): Unit = {
    val initialVDom = engine.askCurrentValues()(mainUi)

    val domPatcherOpt =
      initialVDom.map(v => new DomPatcher(v.render))

    domPatcherOpt match {
      case Some(domPatcher) =>
        def onLoad(x: Any) = {
          val el = dom.document.getElementById("mtfrpcontent")
          el.replaceChild(domPatcher.renderedElement, el.firstChild)
        }
        dom.document.addEventListener("DOMContentLoaded", onLoad _)

        engine.subscribeForPulses { pulses =>
          val newVDomOpt = pulses(mainUi.changes).map(_.render)
          newVDomOpt.foreach { newVDom =>
            domPatcher.applyNewState(newVDom)
          }
        }

      case _ => logger.info(s"Could not create a DomPatcher, no value for: $mainUi")
    }
  }
}
