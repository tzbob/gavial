package mtfrp.core

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import hokko.core.Engine

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.StdIn

trait MyMain extends FrpMain {

  def main(args: Array[String]): Unit = {
    val renderedHtml = ui.initial
    val index        = createIndex(renderedHtml)

    val routeCreator = new RouteCreator(ui.graph)

    implicit val system       = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()

    val totalRoute    = routeCreator.route ~ index ~ resourceRoute

    Http().bindAndHandle(totalRoute, "localhost", 8080)
    println(s"Server online at http://localhost:8080/")
    Await.result(system.whenTerminated, Duration.Inf)
  }

  val resourceRoute: Route = pathPrefix("") {
    getFromResourceDirectory("")
  }

  import UI.html.all._
  def createIndex(content: UI.HTML): Route = {
    path("") {
      get {
        val rawHtml = UI.html.tags.html(
          head(headExtensions),
          body(id := "mtfrpcontent", content)
        )
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, rawHtml.render))
      }
    }
  }

  val headExtensions: List[UI.HTML]
}
