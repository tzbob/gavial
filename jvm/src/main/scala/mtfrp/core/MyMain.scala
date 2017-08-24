package mtfrp.core

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.io.StdIn
import scala.language.dynamics
import scalatags.generic.Attr
import scalatags.text.Builder

trait MyMain extends FrpMain[Builder, String, String] {

  trait Dummy extends Dynamic {
    def selectDynamic(methodName: String): Dummy = ???
    def applyDynamic(methodName: String)(argument: Any): Dummy = ???
  }

  def listen(tag: html.TypedTag[_])(b: ClientBehavior[_])(
      selector: Dummy => Any): html.Tag = ???

  implicit def attrSrc[T] = new html.AttrValue[ClientEvent[T]] {
    override def apply(t: Builder, a: Attr, v: ClientEvent[T]): Unit = ???
  }

  val html = scalatags.Text

  def main(args: Array[String]): Unit = {
    val renderedHtml = ui.initial
    val index        = createIndex(renderedHtml)

    val routeCreator = new RouteCreator(ui.graph)

    implicit val system       = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()

    val totalRoute    = routeCreator.route ~ resourceRoute ~ index
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

  import html.all._
  def createIndex(content: HTML): Route = {
    path("") {
      get {
        val rawHtml = html.tags.html(
          head(
            script(src := "resources/foo-jsdeps.js"),
            script(src := "resources/foo-fastopt.js"),
            script(src := "resources/foo-launcher.js")
          ),
          body(id := "mtfrpcontent", content)
        )
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, rawHtml.render))
      }
    }
  }
}
