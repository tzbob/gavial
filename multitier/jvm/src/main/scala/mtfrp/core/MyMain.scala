package mtfrp.core

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait MyMain extends FrpMain {

  def main(args: Array[String]): Unit = {
    val renderedHtml = ui.initial
    val index        = createIndex(renderedHtml)

    val (route, engine) = if (ui.graph.requiresWebSockets) {
      println("Application requires web sockets.")
      val creator = new WebSocketRouteCreator(ui.graph.replicationGraph)
      (creator.route, creator.engine)
    } else {
      println("Application does not require web sockets, running on XHR.")
      val creator = new XhrRouteCreator(ui.graph.replicationGraph)
      (creator.route, creator.engine)
    }

    implicit val system       = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()

    val totalRoute = route ~ index ~ resourceRoute

    Http().bindAndHandle(totalRoute, "localhost", 8080)
    println(s"Server online at http://localhost:8080/")

    engine.fire(Seq((AppEvent.serverStart, ())))

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
