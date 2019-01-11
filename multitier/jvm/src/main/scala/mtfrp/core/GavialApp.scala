package mtfrp.core

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait GavialApp extends FrpMain {

  def resourceDirectory: String = ""
  def headExtensions: List[UI.HTML]
  def host: String
  def port: Int

  val resourceDirectory: String = ""

  def main(args: Array[String]): Unit = {
    val renderedHtml = ui.initial
    val index        = createIndex(renderedHtml)

    val g = ui.graph

    val (route, engine) = if (g.requiresWebSockets.value) {
      println("Application requires web sockets.")
      val creator = new WebSocketRouteCreator(g.replicationGraph.value)
      (creator.route, creator.engine)
    } else {
      println("Application does not require web sockets, running on XHR.")
      val creator = new XhrRouteCreator(g.replicationGraph.value)
      (creator.route, creator.engine)
    }

    // Run engine effects
    g.effect.value.foreach(_ apply engine)

    implicit val system       = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()

    val totalRoute = route ~ index ~ resourceRoute

    Http().bindAndHandle(totalRoute, host, port)
    println(s"Server online at http://localhost:8080/")

    engine.fire(Seq((AppEvent.serverStart, ())))

    Await.result(system.whenTerminated, Duration.Inf)
  }

  val resourceRoute: Route = pathPrefix("") {
    getFromResourceDirectory(resourceDirectory)
  }

  import UI.html.all._
  def createIndex(content: UI.HTML): Route = {
    path("") {
      get {
        val rawHtml = UI.html.tags.html(
          head(headExtensions),
          body(div(id := "mtfrpcontent", content))
        )
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, rawHtml.render))
      }
    }
  }
}
