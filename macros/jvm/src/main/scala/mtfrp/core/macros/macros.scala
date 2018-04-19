package mtfrp.core.macros

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros

class client extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro EraseForDummy.implementation
}

class server extends StaticAnnotation
