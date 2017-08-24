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

  def main(): Unit = {
    val clientId = ClientGenerator.static.id

    val manager =
      new EventManager(ui.graph, Seq(ui.rep.toCBehavior), Seq(ui.rep.changes))

    manager.startReceiving(s"/${Names.toClientUpdates}/$clientId")
    manager.startSending(s"/${Names.toServerUpdates}/$clientId")

    applyHtml(manager.engine, ui.rep)
  }

  def applyHtml(engine: Engine, mainUi: HC.DBehavior[HTML]): Unit = {
    val initialVDom: Option[HTML] =
      engine.askCurrentValues()(mainUi.toCBehavior)

    val domPatcherOpt =
      initialVDom.map(v => new DomPatcher(v.render(engine)))

    domPatcherOpt match {
      case Some(domPatcher) =>
        def onLoad(x: Any) = {
          val el = dom.document.getElementById("mtfrpcontent")
          el.replaceChild(domPatcher.parent, el.firstChild)
        }
        dom.document.addEventListener("DOMContentLoaded", onLoad _)

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
