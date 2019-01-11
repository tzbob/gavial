package mtfrp.core

import hokko.core.Engine
import hokko.{core => HC}
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import scalatags.hokko.DomPatcher
import slogging.LazyLogging
import snabbdom.VNode

import scala.scalajs.js

trait MyMain extends js.JSApp with FrpMain with LazyLogging {

  override def main(): Unit = {
    setup()
  }

  def setup(): (Engine, EventManager) = {
    val rep      = ui.rep
    val behavior = rep.toCBehavior
    val manager  = new EventManager(ui.graph, Seq(behavior), Seq(rep.changes))
    val engine   = manager.engine
    applyHtml(engine, rep)
    // Run engine effects
    ui.graph.effect.value.foreach(_ apply engine)
    manager.start()
    engine -> manager
  }

  def applyHtml(engine: Engine, behavior: HC.DBehavior[UI.HTML]): Unit = {
    val values      = engine.askCurrentValues()
    val initialVDom = values(behavior.toCBehavior)
    logger.debug(s"Initial VDOM: $initialVDom")

    // Take the (optional) initial vdom to initiate the first patch of the DOM
    initialVDom match {
      case Some(vdom) =>
        def onLoad(x: Any) = {
          val el                     = dom.document.getElementById("mtfrpcontent")
          val domPatcher: DomPatcher = initialRendering(engine, vdom, el)
          patchDomOnChange(domPatcher, engine, behavior.changes)
        }

        dom.document.addEventListener("DOMContentLoaded", onLoad _)
        ()
      case _ =>
        logger.info(s"Could not create initial vdom, no value for: $behavior")
    }
  }

  private def initialRendering(engine: Engine, vdom: UI.HTML, el: Element) = {
    val node       = vdom.render(engine)
    val domPatcher = new DomPatcher(node, Some(el))
    logger.debug(
      s"Created DomPatcher $domPatcher to render $node on ${el.outerHTML}")
    domPatcher
  }

  private def patchDomOnChange(patcher: DomPatcher,
                               engine: Engine,
                               rep: HC.Event[UI.HTML]) = {
    engine.subscribeForPulses { pulses =>
      val newVDomOpt = pulses(rep).map(_.render)
      newVDomOpt.foreach { (newVDom: (Engine) => VNode) =>
        patcher.applyNewState(newVDom(engine))
      }
    }
  }
}
