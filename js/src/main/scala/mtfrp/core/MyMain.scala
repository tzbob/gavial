package mtfrp.core

import hokko.{core => HC}
import org.scalajs.dom

import scala.scalajs.js
import scalatags.vdom.Builder
import scalatags.vdom.raw.VirtualDom.VTreeChild

trait MyMain extends js.JSApp with FrpMain[Builder, VTreeChild, VTreeChild] {
  val html = scalatags.VDom

  def main(): Unit = {
    val clientId = ClientGenerator.static.id

    val engine   = HC.Engine.compile(Nil, Seq(ui.rep))
    val listener = new SseEventListener

    val receiver = new EventReceiver(ui.graph, engine, listener)
    receiver.restart(s"/${Names.exitUpdates}/$clientId")

    val initialVDom = engine.askCurrentValues()(ui.rep)

    val mtfrpContent = dom.document.getElementById("mtfrp-content")
    val domPatcherOpt =
      initialVDom.map(v => new DomPatcher(v.render, Some(mtfrpContent)))

    // FIXME: Log if there is no patcher
    domPatcherOpt.foreach { domPatcher =>
      engine.subscribeForPulses { pulses =>
        val newVDomOpt = pulses(ui.rep.changes).map(_.render)
        newVDomOpt.foreach { newVDom =>
          domPatcher.applyNewState(newVDom)
        }
      }
    }
  }
}
