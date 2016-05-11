package mtfrp.core

import hokko.core._

trait MyMain extends FrpMain {
  import scalatags.Text.all._

  def main(args: Array[String]): Unit = {
    val engine = Engine.compile(Nil, Nil)

    // setup all pipelines towards the client

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
