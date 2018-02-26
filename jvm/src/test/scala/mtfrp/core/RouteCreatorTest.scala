package mtfrp.core

import akka.Done
import akka.http.scaladsl.model.ws
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream._
import akka.stream.scaladsl._
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalatest.{AsyncWordSpec, Matchers}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class RouteCreatorTest
    extends AsyncWordSpec
    with Matchers
    with ScalatestRouteTest {

  "RouteCreator" must {

    "form proper clients on /events/{uuid}" in {
      val rc      = new RouteCreator(ReplicationGraph.start)
      val client0 = ClientGenerator.fresh

      val route = rc.buildRoute { cid =>
        assert(cid !== ClientGenerator.fresh)
        assert(cid === client0)
        Flow[ws.Message]
      }

      val wsClient = WSProbe()
      WS(s"/${Names.ws}/${client0.id.toString}", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(ws.TextMessage.Strict("hello"))
        wsClient.expectMessage("hello")
        assert(isWebSocketUpgrade === true)
      }
    }

    "accept proper input events on sink" in {
      val input = List(1, 2, 3, 4, 5)
        .grouped(2)
        .map(l => l.map(d => Message(1, d.asJson)))
        .toList

      val buffer = ListBuffer.empty[List[Message]]
      val sink: Sink[ws.Message, Future[Done]] =
        RouteCreator.buildInputSinkGeneric { data =>
          buffer += data.toList
          ()
        }

      val wsInput = input.map(i => ws.TextMessage.Strict(i.asJson.noSpaces))

      Source(wsInput).runWith(sink).map { d =>
        assert(d === Done)
        assert(buffer === input)
      }
    }

    "offer messages on the sourcequeue" in {
      val client   = ClientGenerator.fresh
      val source   = Source.queue[ws.Message](10, OverflowStrategy.fail)
      val messages = Seq(Message(1, 20.asJson))

      val offeredSrc = source.mapMaterializedValue { q =>
        RouteCreator.offer(client, q, Some(Map(client -> messages)))
      }

      val expected = ws.TextMessage.Strict(messages.asJson.noSpaces)

      offeredSrc.runWith(Sink.head).map { m =>
        assert(m === expected)
      }
    }

  }
}
