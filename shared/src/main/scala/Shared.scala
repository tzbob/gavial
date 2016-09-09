package mtfrp.core {

  import scalatags.generic.{Bundle, TypedTag}

  trait FrpMain[Builder, Output <: FragT, FragT] {
    val html: Bundle[Builder, Output, FragT]

    type HTML = TypedTag[Builder, Output, FragT]
    def ui: ClientDiscreteBehavior[HTML]
  }

  object MyApp extends MyMain {

    import html.all._
    import hokko.{core => HC}

    val source = HC.Event.source[Int]

    val ev0: AppEvent[Client => Option[Int]] = null

    val ev: AppEvent[Client => Option[Int]] =
      ev0.toClient.map { i =>
        println(s"ev0 toClient: $i")
        i
      }.toApp.map { i => (c: Client) =>
        println(i)
        Some(i._2)
      }
    val ui: ClientDiscreteBehavior[HTML] =
      ev.toClient
        .fold(0) { (acc, n) =>
          println(s"new value from server: $n")
          acc + n
        }
        .toDiscreteBehavior
        .map { x =>
          println(s"discrete update: $x")
          div(p(x))
        }

  }

}
