package mtfrp.core

import hokko.{core => HC}
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalatest.{Matchers, WordSpec}

class EventReceiverTest extends WordSpec with Matchers {

  "EventReceiver" must {

    "define update and reset as handlers" in {
      var set = false
      val listener = new EventListener {
        def restart(url: String, handler: (String) => Unit): Unit = {
          set = true
        }
      }

      val ev1    = AppEvent.toClient(AppEvent.empty[Client => Option[Int]])
      val engine = HC.Engine.compile(ev1.rep)
      new EventReceiver(
        new ReplicationGraphClient(ev1.graph.replicationGraph.value),
        engine,
        listener)
        .restart("test")

      assert(set)
    }

    "decode messages as pulses" in {
      HasToken.reset()

      val ev1 = AppEvent.toClient(AppEvent.empty[Client => Option[Int]])
      val ev2 = AppEvent.toClient(AppEvent.empty[Client => Option[Int]])

      val exit = ev1.unionLeft(ev2)

      val rgc = new ReplicationGraphClient(exit.graph.replicationGraph.value)

      val x        = Message.fromPayload(1)(999)
      val y        = Message.fromPayload(2)(777)
      val xs       = List(x, y, y, x, x)
      val messages = xs.asJson.noSpaces

      val result = EventReceiver.decodeAsPulses(rgc, messages).toOption.get

      assert(result.forall(x => x._2 == 999 || x._2 == 777))
      assert(result(0) == result(3))
      assert(result(0) == result(4))
      assert(result(1) == result(2))
    }
  }

}
