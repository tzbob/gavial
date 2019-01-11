package mtfrp.core

import cats.data.Ior
import hokko.core.EventSource
import hokko.{core => HC}
import mtfrp.core.ReplicationGraph.Pulse
import org.scalatest.WordSpec

class ReplicationGraphClientTest extends WordSpec {

  def makeAppEvent: (HC.EventSource[Int], AppEvent[(Client, Int)]) = {
    val src = HC.Event.source[Int]
    src -> ClientEvent.toAppWithClient(ClientEvent(src, GraphState.default))
  }

  def makeAppBehavior: (EventSource[Int],
                        AppIBehavior[Map[Client, Int],
                                     Ior[(Client, Int), ClientChange]]) = {
    val src = HC.Event.source[Int]
    src -> makeCountingBehavior(ClientEvent(src, GraphState.default))
  }

  def makeCountingBehavior(beh1src: ClientEvent[Int]) = {
    ClientIBehavior.toApp(beh1src.fold(0)(_ + _))
  }

  "ReplicationGraphClientTest" should {

    def makeClientEvent: ClientEvent[Int] =
      AppEvent.toClient(new AppEvent[Client => Option[Int]](GraphState.default))

    def makeClientBehavior: ClientIBehavior[Int, Int] =
      AppIBehavior.toClient(
        new AppIBehavior[Client => Int, Client => Option[Int]](
          GraphState.default,
          null,
          _ => 0,
        ),
        0)

    "build an input event router that deals with events, behavior deltas and behavior resets" in {
      HasToken.reset()

      val beh1 = makeClientBehavior
      val ev1  = makeClientEvent

      val combined = beh1.snapshotWith(ev1) { _ -> _ }

      val router: Message => Option[Pulse] =
        new ReplicationGraphClient(combined.graph.replicationGraph.value).inputEventRouter

      val resetPulse  = router(Message.fromPayload(1)(10))
      val changePulse = router(Message.fromPayload(2)(20))
      val eventPulse  = router(Message.fromPayload(3)(30))

      assert(resetPulse.get._2 === 10)
      assert(changePulse.get._2 === 20)
      assert(eventPulse.get._2 === 30)
    }

    "build an exitEvent with both behavior deltas and events" in {
      HasToken.reset()

      val (ev1src, ev1)      = makeAppEvent
      val (beh1srcsrc, beh1) = makeAppBehavior

      val combined = beh1.toDBehavior.sampledBy(ev1)
      val exitEvent =
        new ReplicationGraphClient(combined.graph.replicationGraph.value).exitEvent

      val engine = HC.Engine.compile(exitEvent)

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
