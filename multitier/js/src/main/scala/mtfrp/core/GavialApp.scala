package mtfrp.core

import hokko.core.Engine
import hokko.{core => HC}
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import scalatags.hokko.DomPatcher
import slogging.LazyLogging
import snabbdom.VNode

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js

trait GavialApp extends js.JSApp with FrpMain with LazyLogging {

  override def main(): Unit = {
    setup()
  }

  def setup(): Unit = {
    val rep      = ui.rep
    val behavior = rep.toCBehavior
    val manager  = new EventManager(ui.graph, Seq(behavior), Seq(rep.changes))
    val onReset  = manager.start()
    val engine   = manager.engine

    applyHtml(engine, rep, onReset)

    // Run engine effects
    ui.graph.effect.value.foreach(_ apply engine)
  }

  def applyHtml(engine: Engine,
                behavior: HC.DBehavior[UI.HTML],
                onReset: Future[Unit]): Unit = {
    val values      = engine.askCurrentValues()
    val initialVDom = values(behavior.toCBehavior)
    logger.debug(s"Initial VDOM: $initialVDom")

    // Take the (optional) initial vdom to initiate the first patch of the DOM
    initialVDom match {
      case Some(vdom) =>
        def onLoad(x: Any) = {
          val el                     = dom.document.getElementById("mtfrpcontent")
          val domPatcher: DomPatcher = initialRendering(engine, vdom, el)

          onReset.foreach { _ =>
            val values            = engine.askCurrentValues()
            val resetWithNewInits = values(behavior.toCBehavior)
            resetWithNewInits.foreach { newInits =>
              logger.info(s"Applying resets $newInits")
              domPatcher.applyNewState(newInits.render(engine))
            }
          }

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
