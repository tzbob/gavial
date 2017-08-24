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
        override def restart(url: String,
                             handlers: Map[String, (String) => Unit]): Unit = {
          assert(handlers.isDefinedAt("update"))
          assert(handlers.isDefinedAt("reset"))
          set = true
        }
      }

      val ev1    = AppEvent.empty[Client => Option[Int]].toClient
      val engine = HC.Engine.compile(ev1.rep)
      new EventReceiver(new ReplicationGraphClient(ev1.graph),
                        engine,
                        listener).restart("test")

      assert(set)
    }

    "decode messages as pulses" in {
      HasToken.reset()

      val ev1 = AppEvent.empty[Client => Option[Int]].toClient
      val ev2 = AppEvent.empty[Client => Option[Int]].toClient

      val exit = ev1.unionLeft(ev2)

      val engine = HC.Engine.compile(exit.rep)

      val rcv =
        new EventReceiver(new ReplicationGraphClient(exit.graph), engine, null)

      val x        = Message.fromPayload(1)(999)
      val y        = Message.fromPayload(2)(777)
      val xs       = List(x, y, y, x, x)
      val messages = xs.asJson.noSpaces

      val xp     = ev1.rep -> 999
      val yp     = ev2.rep -> 777
      val pulses = List(xp, yp, yp, xp, xp)

      val result = rcv.decodeAsPulses(messages).toOption.get

      assert(result.forall(x => x._2 == 999 || x._2 == 777))
      assert(result(0) == result(3))
      assert(result(0) == result(4))
      assert(result(1) == result(2))
    }
  }

}
