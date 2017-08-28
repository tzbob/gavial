package mtfrp.core

import scalatags.generic.{Bundle, TypedTag}

trait FrpMain[Builder, Output <: FragT, FragT] {
  val html: Bundle[Builder, Output, FragT]

  type HTML = TypedTag[Builder, Output, FragT]
  def ui: ClientDiscreteBehavior[HTML]

  val UI: Any
}
