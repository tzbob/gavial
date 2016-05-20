package mtfrp.core

import hokko.{core => HC}

trait MyMain extends FrpMain {
  import scalatags.Text.all._

  def main(args: Array[String]): Unit = {
    val engine = HC.Engine.compile(Nil, Nil)

    // setup all pipelines towards the client

    // This has 4 sections

    // 1. Set up sending events
    // 2. Set up receiving events

    // TODO: Merge all exit and input events into 1 big exit event
    println(ReplicationGraph.exitEvents(ui.graph))

    // TODO: Merge..
    println(ReplicationGraph.exitBehaviors(ui.graph))

    // setup HTML page
    val rawHtml = html(
      head(
        script(src := ".")
      ),
      body(
        ui.initial
      )
    )
    println(rawHtml.render)
  }

}
