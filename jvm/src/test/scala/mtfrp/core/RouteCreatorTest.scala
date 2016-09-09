package mtfrp.core

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream._
import akka.stream.scaladsl._
import de.heikoseeberger.akkasse._
import hokko.{core => HC}
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class RouteCreatorTest extends WordSpec with Matchers with ScalatestRouteTest {

  import EventStreamUnmarshalling._

  "RouteCreator" must {

    "form proper clients on /events/{uuid}" in {
      val source = Source(List.empty)

      val client0 = ClientGenerator.fresh

      val route = RouteCreator.buildExitRoute(source) { (client, src) =>
        assert(client === client0)
        assert(client !== ClientGenerator.fresh)
        src
      }

      Get(s"/${Names.toClientUpdates}/${client0.id.toString}") ~> route ~> check {
        assert(status === StatusCodes.OK)
      }
    }

    "accept proper input events clients on /inputUpdates/{uuid}" in {
      val client0         = ClientGenerator.fresh
      val input: Seq[Int] = Range(1, 20)

      val route = RouteCreator.buildInputRoute[Seq[Int]] { (client, data) =>
        assert(client === client0)
        assert(client !== ClientGenerator.fresh)
        assert(data === input)
        ()
      }

      val post =
        HttpRequest(HttpMethods.POST,
                    uri = s"/${Names.toServerUpdates}/${client0.id.toString}",
                    entity = HttpEntity(
                      akka.http.scaladsl.model.MediaTypes.`application/json`,
                      input.asJson.noSpaces))

      post ~> route ~> check {
        assert(status === StatusCodes.OK)
      }
    }

    "queue the exit event on a sourcequeue" in {
      val eventSource = HC.Event.source[Client => Int]
      val engine      = HC.Engine.compile(List(eventSource.toEvent), Nil)

      val client    = ClientGenerator.fresh
      val queueSize = Int.MaxValue
      val src       = Source.queue[ServerSentEvent](queueSize, OverflowStrategy.fail)
      val mappedSrc =
        RouteCreator.queueUpdates(eventSource.toEvent, engine)(client, src)

      val range  = Range(1, 10)
      val future = mappedSrc.grouped(range.size).runWith(Sink.head)

      range.foreach { x =>
        engine.fire(Seq(eventSource -> ((c: Client) => x)))
      }

      val expectedResult =
        range.map(x => ServerSentEvent(x.toString, Names.Sse.update))

      val result = Await.result(future, 100.millis)
      assert(result === expectedResult)
    }

    "queue the resets on a sourcequeue" in {
      val eventSource = HC.Event.source[Client => Int]
      val init        = 0
      val beh = eventSource.toEvent.fold((c: Client) => init) {
        (accF, newF) => (c: Client) =>
          accF(c) + newF(c)
      }
      val engine = HC.Engine.compile(Nil, Seq(beh.toCBehavior))

      val client    = ClientGenerator.fresh
      val queueSize = Int.MaxValue
      val src       = Source.queue[ServerSentEvent](queueSize, OverflowStrategy.fail)
      val mappedSrc = RouteCreator.queueResets(beh.toCBehavior, engine)(client, src)

      val future = mappedSrc.grouped(1).runWith(Sink.head)

      val expectedResult = Seq(ServerSentEvent(init.toString, Names.Sse.reset))

      val result = Await.result(future, 100.millis)
      assert(result === expectedResult)
    }

    "send messages from the given source on /events/{uuid}" in {
      val msg  = ServerSentEvent("one", "updates")
      val msg2 = ServerSentEvent("two", "resets")

      val source = Source(List(msg, msg, msg, msg2, msg2))

      val client0 = ClientGenerator.fresh

      val route = RouteCreator.buildExitRoute(source) { (_, s) =>
        s
      }

      Get(s"/${Names.toClientUpdates}/${client0.id}") ~> route ~> check {
        assert(status === StatusCodes.OK)

        val events = responseAs[Source[ServerSentEvent, Any]]

        val result = Await.result(
          events.runFold(Vector.empty[ServerSentEvent])(_ :+ _),
          1.second
        )

        assert(result !== Vector(msg, msg, msg))
        assert(result === Vector(msg, msg, msg, msg2, msg2))
      }
    }
  }
}
