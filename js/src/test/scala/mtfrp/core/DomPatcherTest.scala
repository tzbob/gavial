package mtfrp.core

import org.scalajs.dom
import org.scalajs.dom.raw.Element
import org.scalatest.WordSpec

import scalatags.generic.Bundle

class Container[Builder, Output <: FragT, FragT](
    val bundle: Bundle[Builder, Output, FragT]) {
  import bundle.all._

  val divHeadingP = div(
    h1("hello"),
    p("this is a test")
  )

  val divHeadingNoP = div(
    h1("hello")
  )
}

class DomPatcherTest extends WordSpec {

  "DomPatcherTest" should {

    "add and remove appropriate tags when new state is applied" in {
      import scalatags.VDom.all._

      def checkInitial(element: Element): Unit = {
        assert(element.tagName.toLowerCase === "div")
        assert(!element.hasChildNodes())
        ()
      }

      val init = div.render

      val patcher = new DomPatcher(init)
      val element = patcher.renderedElement
      dom.document.body.appendChild(element)

      checkInitial(element)

      patcher.applyNewState(div(h1("Hello")).render)

      assert(element.tagName.toLowerCase() === "div")
      assert(element.children.length === 1)
      assert(element.children(0).tagName.toLowerCase() === "h1")

      patcher.applyNewState(init)
      checkInitial(element)
    }

    "add and remove appropriate tags starting from an initial element" in {
      def checkInitial(element: Element): Unit = {
        assert(element.tagName.toLowerCase() === "div")
        assert(element.childElementCount === 2)
        assert(element.children(0).tagName.toLowerCase() === "h1")
        assert(element.children(1).textContent === "this is a test")
        ()
      }

      val jsContainer = new Container(scalatags.JsDom)
      val element     = jsContainer.divHeadingP.render
      checkInitial(element)

      val vdomContainer = new Container(scalatags.VDom)
      val patcher =
        new DomPatcher(vdomContainer.divHeadingP.render, Some(element))

      patcher.applyNewState(vdomContainer.divHeadingNoP.render)
      assert(element.childElementCount === 1)

      patcher.applyNewState(vdomContainer.divHeadingP.render)
      checkInitial(element)
    }

  }
}
