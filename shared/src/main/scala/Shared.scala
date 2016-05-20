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
  val behavior = ApplicationDiscreteBehavior.constant("hello")
  val ui: ClientDiscreteBehavior[HTML] = behavior.toServer.map(x => div(p(x)))
}
