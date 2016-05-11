package mtfrp.core {
  import scalatags.Text.TypedTag

  trait FrpMain {
    type HTML = TypedTag[String]
    def ui: ClientDiscreteBehavior[HTML]
  }

}

import mtfrp.core._
import scalatags.Text.all._

object MyApp extends MyMain {

  val x: ServerEvent[Int] = null

  val behavior = ServerDiscreteBehavior.constant(div(p("hello")))
  val ui: ClientDiscreteBehavior[HTML] = behavior.replicate
}
