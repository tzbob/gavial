package mtfrp.core

import hokko.{core => HC}
import org.scalatest.WordSpec

// FIXME: Share test scenarios between client/server impl of exitEvent
class ReplicationGraphClientTest extends WordSpec {

  def makeAppEvent: (HC.EventSource[Int], AppEvent[(Client, Int)]) = {
    val src = HC.Event.source[Int]
    src -> ClientEvent(src.toEvent).toApp
  }

  def makeAppBehavior: (HC.EventSource[Int],
                        AppIncBehavior[Map[Client, Int], (Client, Int)]) = {
    val src = HC.Event.source[Int]
    src -> makeCountingBehavior(ClientEvent(src.toEvent))
  }

  def makeCountingBehavior(beh1src: ClientEvent[Int])
    : AppIncBehavior[Map[Client, Int], (Client, Int)] = {
    beh1src.fold(0)(_ + _).toApp
  }

  "ReplicationGraphClientTest" should {

    "build an exitEvent with both behavior deltas and events" in {
      HasToken.reset()

      val (ev1src, ev1)      = makeAppEvent
      val (beh1srcsrc, beh1) = makeAppBehavior

      val combined  = beh1.toDiscreteBehavior.sampledBy(ev1)
      val exitEvent = new ReplicationGraphClient(combined.graph).exitEvent

      val engine = HC.Engine.compile(Seq(exitEvent), Nil)

      var fired = false
      engine.subscribeForPulses { pulses =>
        fired = true
        pulses(exitEvent) match {
          case Some(msgs) =>
            assert(
              msgs.toSet === Set(
                Message.fromPayload(1)(1), // id:1 (ev1.toClient)
                Message.fromPayload(3)(1))) // id:3 (ev1.toClient, beh1src.toClient, internal.deltaSender)
            ()
          case _ => ???
        }

      }
    }

  }
}
