package mtfrp.core.macros

import scala.language.implicitConversions
import scala.language.dynamics

object Dummy {
  implicit def dummyToNothing(dummy: Dummy): Nothing =
    null.asInstanceOf[Nothing]
}

class Dummy extends Dynamic {
  def selectDynamic(methodName: String): Dummy                 = new Dummy
  def applyDynamic(methodName: String)(arguments: Any*): Dummy = new Dummy
}
