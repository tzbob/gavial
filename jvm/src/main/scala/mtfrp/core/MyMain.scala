package mtfrp.core

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import hokko.core.Engine

import scala.concurrent.Future
import scala.io.StdIn

trait MyMain extends FrpMain {

  def main(args: Array[String]): Unit = {
    setup
    ()
  }

  private[core] lazy val setup: Engine = {
    val renderedHtml = ui.initial
    val index        = createIndex(renderedHtml)

    val routeCreator = new RouteCreator(ui.graph)

    implicit val system       = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()

    val totalRoute    = routeCreator.route ~ resourceRoute ~ index
    val bindingFuture = Http().bindAndHandle(totalRoute, "localhost", 8080)

    startServer(bindingFuture)
    routeCreator.engine
  }

  def startServer(bindingFuture: Future[ServerBinding])(
      implicit system: ActorSystem): Unit = {
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

  val resourceRoute: Route = pathPrefix("resources") {
    getFromResourceDirectory("")
  }

  import UI.html.all._
  def createIndex(content: UI.HTML): Route = {
    path("") {
      get {
        val rawHtml = UI.html.tags.html(
          head(
            script(
              src := javascriptResults
            )
          ),
          body(id := "mtfrpcontent", content)
        )
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, rawHtml.render))
      }
    }
  }

  val javascriptResults: String
}
