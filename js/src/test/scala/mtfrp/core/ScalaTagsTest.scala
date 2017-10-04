package mtfrp.core

import hokko.core.Engine
import hokko.{core => HC}
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLButtonElement
import org.scalatest.{Matchers, WordSpec}

class ScalaTagsTest extends WordSpec with Matchers {

  "UI in Main" should {

    def setupTestBody(main: MyMain) = {
      val container = dom.document.createElement("div")
      container.setAttribute("id", "mtfrpcontent")
      dom.document.body.appendChild(container)

      val ui          = main.ui
      val clientGraph = new ReplicationGraphClient(ui.graph)
      val engine = Engine.compile(clientGraph.exitEvent,
                                  ui.rep,
                                  ui.rep.toCBehavior,
                                  ui.rep.changes)
      main.applyHtml(engine, ui.rep, false)
    }

    "should push events from HTML" in {
      val exampleApp = new MyMain {
        import html.all._

        def mkButton(txt: String, src: ClientEventSource[Int], v: Int) =
          button(UI.listen(onclick, src)(_ => v), txt)

        val numbers  = ClientEvent.source[Int]
        val addition = mkButton("+", numbers, 1)

        lazy val ui =
          numbers
            .fold(0)(_ + _)
            .toDiscreteBehavior
            .map(nr => div(nr, addition))
      }

      setupTestBody(exampleApp)

      val container = dom.document.getElementById("mtfrpcontent")

      assert(container.innerHTML == "<div>0<button>+</button></div>")

      val btn = dom.document.body.querySelector("button")
      btn.asInstanceOf[HTMLButtonElement].click()

      assert(container.innerHTML == "<div>1<button>+</button></div>")
    }

    "should read properties from HTML" in {
      val exampleApp = new MyMain {
        import html.all._

        val event = ClientEvent.source[Unit]
        val btn   = button(UI.listen(onclick, event)(_ => ()), "Trigger")

        val intSink       = ClientBehavior.sink(10000)
        val twentyDivRead = UI.read(div(btn))(intSink, _.childElementCount)

        val snapped = intSink.snapshotWith(event) { (int, _) =>
          int
        }
        val folded = snapped.fold(0)(_ + _).toDiscreteBehavior

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
