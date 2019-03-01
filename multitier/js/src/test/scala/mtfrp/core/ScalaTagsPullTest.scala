package mtfrp.core

import org.scalajs.dom
import org.scalajs.dom.raw.HTMLButtonElement
import org.scalatest.{Matchers, WordSpec}
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

import scala.scalajs.js

class ScalaTagsPullTest extends WordSpec with Matchers {

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
    "push multiple events from counter" in {
      class Counter extends GavialApp {
        import UI.html.all._

        val inc = 1
        val dec = -1

        private[this] val incInput = ClientEvent.source[Int]
        private[this] val decInput = ClientEvent.source[Int]

        private def mkButton(src: ClientEventSource[Int], txt: String, v: Int) =
          button(UI.listen(onclick, src)(_ => v), txt)

        val incButton = mkButton(incInput, "Increment", inc)
        val decButton = mkButton(decInput, "Decrement", dec)

        val state =
          incInput.unionWith(decInput)(_ + _).fold(0)(_ + _)

        val ui: ClientDBehavior[UI.HTML] = state.map(
          v =>
            div(
              div("Current count: ", span(v)),
              div(incButton, decButton)
          ))
      }

      val exampleApp = new Counter

      val engine = setupTestBody(exampleApp)

      def currentValue =
        engine.askCurrentValues()(exampleApp.state.rep.toCBehavior).get

      val btns = dom.document.body.querySelectorAll("button")
      val inc  = btns(0).asInstanceOf[HTMLButtonElement]

      assert(currentValue == 0)

      inc.click()
      inc.click()
      assert(currentValue == 2)

      val dec = btns(1).asInstanceOf[HTMLButtonElement]
      dec.click()
      assert(currentValue == 1)
    }
  }

}
