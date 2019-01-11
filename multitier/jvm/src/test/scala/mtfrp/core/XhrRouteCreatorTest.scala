package mtfrp.core

import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalatest.WordSpec
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

class XhrRouteCreatorTest extends WordSpec with ScalatestRouteTest {
  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.DEBUG

  "The Xhr service" must {

    "return decode error when posting nothing" in {
      val creator = new XhrRouteCreator(ReplicationGraph.start)

      Post(s"/${Names.xhr}/${ClientGenerator.static.id}") ~> creator.route ~>
        check {
          assert(responseAs[String] === "exhausted input")
        }
    }

    "return 'no pulses' error" in {
      val creator  = new XhrRouteCreator(ReplicationGraph.start)
      val messages = List(Message(1, "".asJson)).asJson.noSpaces

      Post(s"/${Names.xhr}/${ClientGenerator.static.id}", messages) ~> creator.route ~>
        check {
          assert(responseAs[String] === "No pulses as result.")
        }
    }

    "return fold result" in {
      HasToken.reset()

      val clientEvent = new ClientEvent[Int](GraphState.default)
      val appBehavior: AppIBehavior[Int, Int] =
        ClientEvent.toApp(clientEvent).fold(0) { (old, n) =>
          old + n
        }
      val clientBehavior =
        SessionIBehavior.toClient(AppIBehavior.toSession(appBehavior))

      val creator =
        new XhrRouteCreator(clientBehavior.graph.replicationGraph.value)

      val messages = List(Message(1, "1".asJson)).asJson.noSpaces
      val client   = ClientGenerator.static

      val one = creator.handleRequest(client, messages)
      assert(one === Right(List(Message(4, 1.asJson))))
    }
  }

}
