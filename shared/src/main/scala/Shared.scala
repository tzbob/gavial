package mtfrp.core {

  import scalatags.generic.{Bundle, TypedTag}

  trait FrpMain[Builder, Output <: FragT, FragT] {
    val html: Bundle[Builder, Output, FragT]

    type HTML = TypedTag[Builder, Output, FragT]
    def ui: ClientDiscreteBehavior[HTML]
  }

  object MyApp extends MyMain {
    import html.all._

    // 1: HTML Pre-render on the server from ClientBehavior.initial
    // 2: Insert 'newest' values for the client, use these as initials for ClientBehaviors & redraw! (skippable)
    // 3: Wait for actual newest values from the server and redraw again (application is now usable!)


    // val ev: AppEvent[Client => Option[String]] = AppEvent.empty
    // val behavior: AppIncBehavior[Client => String, Client => Option[String]] = ev.fold((c: Client) => "") {
    //   (f1, f2) => c: Client => f1(c) + f2(c).getOrElse("")
    // }
    // val ui: ClientDiscreteBehavior[HTML] = behavior.toClient.discreteMap((x: String) => div(p(x)))

    import hokko.{core => HC}
    val source = HC.Event.source[Int]

    val ev: AppEvent[Client => Option[Int]] =
      AppEvent(source).map(i => (c: Client) => Some(i))
    val ui: ClientDiscreteBehavior[HTML] = ev.toClient
      .unionLeft(ev.toClient)
      .fold(0)(_ + _)
      .discreteMap(x => div(p(x)))
  }

}
