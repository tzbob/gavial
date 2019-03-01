package mtfrp.core

import hokko.core.Engine
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLButtonElement
import org.scalatest.{Matchers, WordSpec}
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

import scala.scalajs.js

class ScalaTagsPushTest extends WordSpec with Matchers {

  def setupTestBody(main: => GavialApp) = {

    val el = dom.document.body
    while (el.childElementCount > 1) el.removeChild(el.lastChild)

    val parent = dom.document.createElement("div")
    parent.setAttribute("id", "parento")
    val container = dom.document.createElement("div")
    container.setAttribute("id", "mtfrpcontent")
    parent.appendChild(container)
    dom.document.body.appendChild(parent)

    assert(dom.document.getElementById("mtfrpcontent") !== null)
    assert(dom.document.getElementById("mtfrpcontent") !== js.undefined)

    val (engine, _) = main.setup()

    val event =
      js.Dynamic.newInstance(js.Dynamic.global.Event)("DOMContentLoaded",
                                                      js.Dynamic.literal(
                                                        bubbles = true,
                                                        cancelable = true
                                                      ))
    dom.window.document.dispatchEvent(event.asInstanceOf[dom.raw.Event])

    engine
  }

  "UI in Main" should {
    "read properties from HTML" in {
      val exampleApp = new GavialApp {
        import UI.html.all._

        val event = ClientEvent.source[Unit]
        val btn   = button(UI.listen(onclick, event)(_ => ()), "Trigger")

        val intSink = ClientBehavior.sink(10000)
        val twentyDivRead = UI.read(div(btn))(intSink,
                                              _.childElementCount
                                                .asInstanceOf[Int])

        val snapped = intSink.snapshotWith(event) { (int, _) =>
          int
        }
        val folded = snapped.fold(0)(_ + _)

        lazy val ui = folded.map { x =>
          div(x, twentyDivRead)
        }
      }

      setupTestBody(exampleApp)

      val container = dom.document.getElementById("parento")

      assert(
        container.innerHTML ==
          "<div>0<div><button>Trigger</button></div></div>")

      val btn = dom.document.body.querySelector("button")
      btn.asInstanceOf[HTMLButtonElement].click()

      assert(
        container.innerHTML ==
          "<div>1<div><button>Trigger</button></div></div>")
    }

  }

}
