package mtfrp.core

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import de.heikoseeberger.akkasse._

import org.scalatest.WordSpec
import org.scalatest.Matchers

import akka.http.scaladsl.server._
import akka.stream._
import akka.stream.scaladsl._

import java.util.UUID

import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import hokko.{core => HC}

class RouteCreatorTest extends WordSpec with Matchers with ScalatestRouteTest {

  import EventStreamUnmarshalling._

  "RouteCreator" must {
    def newUuid = UUID.randomUUID()

    "form proper clients on /events/{uuid}" in {
      val source = Source(List.empty)

      val uuid = newUuid

      val route = RouteCreator.buildExitRoute(source) { (client, src) =>
        assert(client === Client(uuid))
        assert(client !== Client(newUuid))
        src
      }

      Get(s"/events/$uuid") ~> route ~> check {
        assert(status === StatusCodes.OK)
      }
    }

    "queue the exit event on a sourcequeue" in {
      val eventSource = HC.Event.source[Client => Int]
      val engine = HC.Engine.compile(List(eventSource), Nil)

      val client = Client(newUuid)
      val queueSize = Int.MaxValue
      val src = Source.queue[Int](queueSize, OverflowStrategy.fail)
      val mappedSrc = RouteCreator.queueEvent(eventSource, engine)(client, src)

      val range = Range(1, 10)

      val future = mappedSrc.grouped(range.size).runWith(Sink.head)

      range.foreach { x =>
        engine.fire(Seq(eventSource -> ((c: Client) => x)))
      }

      val result = Await.result(future, 100.millis)
      assert(result !== Range(1, 3))
      assert(result === range)
    }

    "send messages from the given source on /events/{uuid}" in {
      val msg = Message.fromPayload(1)("one")
      val msg2 = Message.fromPayload(2)("two")

      val source = Source(List(Seq(msg, msg, msg), Seq(msg2, msg2)))

      val uuid = newUuid

      val route = RouteCreator.buildExitRoute(source) { (_, s) =>
        s
      }

      Get(s"/events/$uuid") ~> route ~> check {
        assert(status === StatusCodes.OK)

        val events = responseAs[Source[ServerSentEvent, Any]].collect {
          case ServerSentEvent(data, None, _, _) =>
            decode[Seq[Message]](data).toOption.get
        }

        val result = Await.result(
          events.runFold(Vector.empty[Seq[Message]])(_ :+ _),
          1.second
        )

        assert(result !== Vector(Seq(msg, msg, msg)))
        assert(result === Vector(Seq(msg, msg, msg), Seq(msg2, msg2)))
      }
    }
  }
}
