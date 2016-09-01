package mtfrp.core

import org.scalajs.dom.raw.Element

import scalatags.vdom.raw.VirtualDom
import scalatags.vdom.raw.VirtualDom.VTreeChild

class DomPatcher(initialVDom: VTreeChild,
                 preRenderedElement: Option[Element] = None) {
  val renderedElement =
    preRenderedElement.getOrElse(VirtualDom.create(initialVDom))
  private[this] var previousState = initialVDom

  private[this] def diffAndSwap(vdom: VTreeChild): VirtualDom.Patch = {
    val patch = VirtualDom.diff(previousState, vdom)
    previousState = vdom
    patch
  }

  def applyNewState(vdom: VTreeChild): Element = {
    val patch = diffAndSwap(vdom)
    VirtualDom.patch(renderedElement, patch)
  }
}
