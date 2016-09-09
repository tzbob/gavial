package mtfrp.core

import hokko.{core => HC}
import mtfrp.core.ReplicationGraph.Pulse
import org.scalatest.{Matchers, WordSpec}

class ReplicationGraphServerTest extends WordSpec with Matchers {

  def makeClientEvent: (HC.EventSource[Client => Option[Int]],
                        ClientEvent[Int]) = {
    val src = HC.Event.source[Client => Option[Int]]
    src -> AppEvent(src.toEvent).toClient
  }

  def makeClientBehavior: (HC.EventSource[Client => Option[Int]],
                           ClientIncBehavior[Int, Int]) = {
    val src = HC.Event.source[Client => Option[Int]]
    src -> makeCountingBehavior(AppEvent(src.toEvent))
  }

  def makeCountingBehavior(beh1src: AppEvent[Client => Option[Int]])
    : ClientIncBehavior[Int, Int] = {
    beh1src
      .fold((c: Client) => 0) { (accF, newF) => (c: Client) =>
        accF(c) + newF(c).getOrElse(0)
      }
      .toClient
  }

  "ReplicationGraphServerTest" should {

    "build an exitEvent with both behavior deltas and events" in {
      HasToken.synchronized {
        HasToken.reset()

        val (ev1src, ev1)      = makeClientEvent
        val (beh1srcsrc, beh1) = makeClientBehavior

        val combined  = beh1.toDiscreteBehavior.sampledBy(ev1)
        val exitEvent = new ReplicationGraphServer(combined.graph).exitEvent

        val engine = HC.Engine.compile(Seq(exitEvent), Nil)

        var fired = false
        engine.subscribeForPulses { pulses =>
          fired = true
          pulses(exitEvent) match {
            case Some(msgs) =>
              val toSet = msgs(ClientGenerator.fresh).toSet
              assert(
                toSet === Set(
                  Message.fromPayload(1)(1), // id:1 (ev1.toClient)
                  Message.fromPayload(3)(1))) // id:3 (ev1.toClient, beh1src.toClient, internal.deltaSender)
              ()
            case _ => ???
          }
        }

        val always1 = (_: Any) => Some(1): Option[Int]

        engine.fire(Seq(ev1src -> always1, beh1srcsrc -> always1))

        assert(fired)
      }
    }

    "build an exitBehavior with all behaviors" in {
      HasToken.synchronized {
        HasToken.reset()

        val (beh1srcsrc, beh1) = makeClientBehavior
        val (beh2srcsrc, beh2) = makeClientBehavior

        val combined = beh1.changes.unionLeft(beh2.changes)
        val exitBehavior =
          new ReplicationGraphServer(combined.graph).exitBehavior

        val engine = HC.Engine.compile(Seq.empty, Seq(exitBehavior))

        val value = engine.askCurrentValues() apply exitBehavior
        val toSet = value.get(ClientGenerator.fresh).toSet
        assert(
          toSet === Set(Message.fromPayload(1)(0), Message.fromPayload(3)(0)))
      }
    }

    def makeAppEvent: AppEvent[(Client, Int)] = {
      val ev = new ClientEvent[Int](ReplicationGraph.start)
      ev.toApp
    }

    def makeAppBehavior: AppIncBehavior[Map[Client, Int], (Client, Int)] = {
      val ev =
        new ClientIncBehavior[Int, Int](ReplicationGraph.start, _ + _, 0)
      ev.toApp
    }

    "build an input event router that deals with events, behavior deltas and behavior resets" in {
      HasToken.reset()

      val beh1 = makeAppBehavior
      val ev1  = makeAppEvent

      val combined = beh1.snapshotWith(ev1) { _ -> _ }

      val router: (Client, Message) => Option[Pulse] =
        new ReplicationGraphServer(combined.graph).inputEventRouter

      val fresh = ClientGenerator.fresh

      val changePulse = router(fresh, Message.fromPayload(2)(20))
      val eventPulse  = router(fresh, Message.fromPayload(3)(30))

      assert(changePulse.get._2 === (fresh -> 20))
      assert(eventPulse.get._2 === (fresh  -> 30))
    }

  }

}
