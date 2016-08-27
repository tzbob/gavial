package mtfrp.core

import hokko.{core => HC}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn

trait MyMain extends FrpMain {
  import scalatags.Text.all._

  def main(args: Array[String]): Unit = {
    val exit = ReplicationGraph.exitData(ui.graph)
    val engine = HC.Engine.compile(Seq(exit.event), Nil)

    val route = RouteCreator.exitRoute(exit)(engine)

    val resources = pathPrefix("resources") {
      getFromResourceDirectory("")
    }

    val index = path("") {
      get {
        val rawHtml = html(
          head(
            script(src := "resources/foo-fastopt.js"),
            script(src := "resources/foo-launcher.js")
          ),
          body(
            h1("test")
          )
        )
        rawHtml.render
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, rawHtml.render))
      }
    }

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val totalRoute = route ~ resources ~ index
    val bindingFuture = Http().bindAndHandle(totalRoute, "localhost", 8080)

    println(
      s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

}
