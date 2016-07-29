package mtfrp.core {
  import scalatags.Text.TypedTag

  trait FrpMain {
    type HTML = TypedTag[String]
    def ui: ClientDiscreteBehavior[HTML]
  }

}

import mtfrp.core._
import scalatags.Text.all._
import io.circe._

object MyApp extends MyMain {
  val ev: ApplicationEvent[Client => Option[String]] = ???
  val behavior: ApplicationIncBehavior[Client => String, Client => Option[String]] = ev.fold((c: Client) => "") {
    (f1, f2) => c: Client => f1(c) + f2(c).getOrElse("")
  }
  val ui: ClientDiscreteBehavior[HTML] = behavior.toClient.discreteMap((x: String) => div(p(x)))
}
