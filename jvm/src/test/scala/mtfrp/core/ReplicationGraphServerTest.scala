package mtfrp.core

import hokko.core.Engine
import hokko.{core => HC}
import mtfrp.core.ReplicationGraph.Pulse
import org.scalatest.{Matchers, WordSpec}

class ReplicationGraphServerTest extends WordSpec with Matchers {

  def makeClientEvent
    : (HC.EventSource[Client => Option[Int]], ClientEvent[Int]) = {
    val src = HC.Event.source[Client => Option[Int]]
    src -> AppEvent.toClient(AppEvent(src))
  }

  def makeClientBehavior
    : (HC.EventSource[Client => Option[Int]], ClientIBehavior[Int, Int]) = {
    val src = HC.Event.source[Client => Option[Int]]
    src -> makeCountingBehavior(AppEvent(src))
  }

  def makeCountingBehavior(
      beh1src: AppEvent[Client => Option[Int]]): ClientIBehavior[Int, Int] = {
    AppIBehavior.toClient(beh1src
                            .fold((c: Client) => 0) {
                              (accF, newF) => (c: Client) =>
                                accF(c) + newF(c).getOrElse(0)
                            },
                          0)
  }

  "ReplicationGraphServerTest" should {

    "build an exitEvent with both behavior deltas and events" in {
      HasToken.synchronized {
        HasToken.reset()

        val (ev1src, ev1)      = makeClientEvent
        val (beh1srcsrc, beh1) = makeClientBehavior

        val combined  = beh1.toDBehavior.sampledBy(ev1)
        val exitEvent = new ReplicationGraphServer(combined.graph).exitEvent

        val engine = HC.Engine.compile(exitEvent)

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

        val engine = HC.Engine.compile(exitBehavior)

        val value = engine.askCurrentValues() apply exitBehavior
        val toSet = value.get(ClientGenerator.fresh).toSet
        assert(
          toSet === Set(Message.fromPayload(1)(0), Message.fromPayload(3)(0)))
      }
    }

    def makeAppEvent: AppEvent[(Client, Int)] = {
      val ev = new ClientEvent[Int](ReplicationGraph.start)
      ClientEvent.toApp(ev)
    }

    def makeAppBehavior: AppIBehavior[Map[Client, Int], (Client, Int)] = {
      val ev =
        new ClientIBehavior[Int, Int](ReplicationGraph.start, _ + _, 0)
      ClientIBehavior.toApp(ev)
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

    "not execute a fold multiple times for resets in Session.toClient" in {
      testDuplicateFolds { (src, eff) =>
        val event: SessionEvent[Int] =
          new SessionEvent(new AppEvent(src, ReplicationGraph.start))

        val behavior: SessionIBehavior[Int, Int] = event.fold(0) { (acc, n) =>
          eff()
          acc + n
        }
        SessionIBehavior.toClient(behavior)
      }
    }

    "not execute a fold multiple times for resets in AppIBehavior.broadcast" in {
      testDuplicateFolds { (src, eff) =>
        val event: AppEvent[Map[Client, Int]] =
          new AppEvent(src, ReplicationGraph.start)
        val behavior: AppIBehavior[Int, Int] =
          event.map(_.values.head).fold(0) { (acc, n) =>
            eff()
            acc + n
          }
        AppIBehavior.broadcast(behavior)
      }

    }

    def testDuplicateFolds(
        mkClientIBehavior: (HC.EventSource[Map[Client, Int]],
                            () => Unit) => ClientIBehavior[Int, _]) = {
      HasToken.synchronized {
        HasToken.reset()
        var counter = 0

        val src = HC.Event.source[Map[Client, Int]]

        val client = ClientGenerator.fresh
        val plus1  = Map(client -> 1)

        val clientIBehavior = mkClientIBehavior(src, () => counter += 1)

        val repGraphServer = new ReplicationGraphServer(clientIBehavior.graph)
        val engine = HC.Engine.compile(repGraphServer.exitEvent,
                                       repGraphServer.exitBehavior)

        engine.fire(Seq(AppEvent.clientChangesSource -> Connected(client)))
        engine.fire(Seq(src                          -> plus1))

        def poll(): Seq[Message] = {
          val values: Engine.Values = engine.askCurrentValues()
          val exitBehaviorValue: Option[Client => Seq[Message]] =
            values(repGraphServer.exitBehavior)
          exitBehaviorValue.get(client)
        }

        val res = poll()
        assert(res.size == 1)
        assert(counter == 1)

        poll()
        assert(counter == 1)

        engine.fire(Seq(src -> plus1))
        poll()
        assert(counter == 2)
      }
    }

  }

}
