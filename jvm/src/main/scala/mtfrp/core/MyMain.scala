package mtfrp.core

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import hokko.{core => HC}

import scala.concurrent.Future
import scala.io.StdIn
import scalatags.text.Builder

trait MyMain extends FrpMain[Builder, String, String] {
  val html = scalatags.Text
  import html.all._

  def main(args: Array[String]): Unit = {
    val renderedHtml = ui.initial
    val index        = createIndex(renderedHtml)

    val exit   = new ReplicationGraphServer(ui.graph).exitData
    val engine = HC.Engine.compile(Seq(exit.event), Nil)

    val exitRoute = RouteCreator.exitRoute(exit)(engine)

    implicit val system       = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()

    val totalRoute    = exitRoute ~ resourceRoute ~ index
    val bindingFuture = Http().bindAndHandle(totalRoute, "localhost", 8080)

    startServer(bindingFuture)
  }

  def startServer(bindingFuture: Future[ServerBinding])(
      implicit system: ActorSystem): Unit = {
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    println(
      s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

  val resourceRoute: Route = pathPrefix("resources") {
    getFromResourceDirectory("")
  }

  def createIndex(content: HTML): Route = {
    path("") {
      get {
        val rawHtml = html.tags.html(
          head(
            script(src := "resources/foo-fastopt.js"),
            script(src := "resources/foo-launcher.js")
          ),
          body(content)
        )
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, rawHtml.render))
      }
    }
  }
}
