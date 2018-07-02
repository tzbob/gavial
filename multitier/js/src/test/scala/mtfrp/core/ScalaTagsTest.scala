package mtfrp.core

import hokko.core.Engine
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLButtonElement
import org.scalatest.{Matchers, WordSpec}

import scala.scalajs.js

class ScalaTagsTest extends WordSpec with Matchers {

  "UI in Main" should {

    def setupTestBody(main: MyMain) = {
      val container = dom.document.createElement("div")
      container.setAttribute("id", "mtfrpcontent")
      dom.document.body.appendChild(container)

      assert(dom.document.getElementById("mtfrpcontent") !== null)
      assert(dom.document.getElementById("mtfrpcontent") !== js.undefined)

      val ui          = main.ui
      val clientGraph = new ReplicationGraphClient(ui.graph.replicationGraph)
      val engine = Engine.compile(clientGraph.exitEvent,
                                  ui.rep,
                                  ui.rep.toCBehavior,
                                  ui.rep.changes)
      main.applyHtml(engine, ui.rep)
      engine
    }

    "push events from HTML" in {
      val exampleApp = new MyMain {
        import UI.html.all._

        def mkButton(txt: String, src: ClientEventSource[Int], v: Int) =
          button(UI.listen(onclick, src)(_ => v), txt)

        val numbers  = ClientEvent.source[Int]
        val addition = mkButton("+", numbers, 1)

        lazy val ui =
          numbers
            .fold(0)(_ + _)
            .toDBehavior
            .map(nr => div(nr, addition))
      }

      setupTestBody(exampleApp)

      val container = dom.document.getElementById("mtfrpcontent")

      assert(container.innerHTML == "<div>0<button>+</button></div>")

      val btn = dom.document.body.querySelector("button")
      btn.asInstanceOf[HTMLButtonElement].click()

      assert(container.innerHTML == "<div>1<button>+</button></div>")
    }

    "push multiple events from counter" in {
      class Counter extends MyMain {
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
          incInput.unionWith(decInput)(_ + _).fold(0)(_ + _).toDBehavior

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

    "read properties from HTML" in {
      val exampleApp = new MyMain {
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
        val folded = snapped.fold(0)(_ + _).toDBehavior

        lazy val ui = folded.map { x =>
          div(x, twentyDivRead)
        }
      }

      setupTestBody(exampleApp)

      val container = dom.document.getElementById("mtfrpcontent")

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
