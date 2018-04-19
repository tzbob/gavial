package mtfrp.core.macros

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros

class server extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro EraseForDummy.implementation
}

class client extends StaticAnnotation
