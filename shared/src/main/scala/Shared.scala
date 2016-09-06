package mtfrp.core {

  import scalatags.generic.{Bundle, TypedTag}

  trait FrpMain[Builder, Output <: FragT, FragT] {
    val html: Bundle[Builder, Output, FragT]

    type HTML = TypedTag[Builder, Output, FragT]
    def ui: ClientDiscreteBehavior[HTML]
  }

  object MyApp extends MyMain {

    // 1: HTML Pre-render on the server from ClientBehavior.initial
    // 2: Insert 'newest' values for the client, use these as initials for ClientBehaviors & redraw! (skippable)
    // 3: Wait for actual newest values from the server and redraw again (application is now usable!)

    import html.all._

    import hokko.{core => HC}
    val source = HC.Event.source[Int]

    val ev0: AppEvent[Client => Option[Int]] =
      AppEvent(source).map { i => (c: Client) =>
        println(s"App Source: $i")
        Some(i)
      }

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
        .discreteMap { x =>
          println(s"discrete update: $x")
          div(p(x))
        }

  }

}
