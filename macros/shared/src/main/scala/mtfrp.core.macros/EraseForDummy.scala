package mtfrp.core.macros

import scala.language.experimental.macros
import reflect.macros.whitebox

object EraseForDummy {
  def implementation(c: whitebox.Context)(
      annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val dummy = q"""
    {
    new mtfrp.core.macros.Dummy {}
    }
    """

    val result = annottees.map(_.tree).toList match {
      case (cc @ q"$mods val $name: $tpeTree = $expr") :: Nil =>
        q"$mods val $name: mtfrp.core.macros.Dummy = $dummy"
      case (cc @ q"$mods var $name: $tpeTree = $expr") :: Nil =>
        q"$mods var $name: mtfrp.core.macros.Dummy = $dummy"
    }

    c.Expr[Any](result)
  }
}
