package mtfrp.core

import mtfrp.core.ReplicationGraph.{BehaviorClientToServer, EventServerToClient}
import mtfrp.core.macros.{client, server}
import org.scalatest.WordSpec

import scala.language.reflectiveCalls

class DBehaviorTest extends WordSpec {

  "DBehaviors" can {

    def testAndReset(a: GraphState)(testObj: { var test: Boolean }) = {
      assert(!testObj.test)
      a.effect.value.foreach(_(null))
      assert(testObj.test)
      testObj.test = false
    }

    "be defined recursively in the App tier" in {

      object TestA {
        var test = false
        val emptyEvent = AppEvent.sourceWithEngineEffect[Int] { fire =>
          test = true
        }
        val count = emptyEvent.fold(1)(_ + _).toDBehavior

        val delayed: AppBehavior[Int] = AppDBehavior.delayed(result)
        val dd = delayed
          .sampledBy(emptyEvent: AppEvent[Int])
          .fold(10)(_ + _)
          .toDBehavior
        val result = dd.map2(count)(_ + _)

        val client = SessionEvent.toClient(AppEvent.toSession(result.changes))
      }

      assert(TestA.result.initial === 11)
      assert(
        TestA.client.graph.replicationGraph.value
          .isInstanceOf[EventServerToClient])

      @server val _ = {
        testAndReset(TestA.emptyEvent.graph)(TestA)
        testAndReset(TestA.count.graph)(TestA)
        testAndReset(TestA.result.graph)(TestA)
        testAndReset(TestA.client.graph)(TestA)
      }
    }

    "be defined recursively in the Client tier" in {
      object TestC {
        var test = false
        val emptyEvent = ClientEvent.sourceWithEngineEffect[Int] { fire =>
          test = true
        }
        val count = emptyEvent.fold(1)(_ + _).toDBehavior

        val delayed: ClientBehavior[Int] = ClientDBehavior.delayed(result)
        val dd = delayed
          .sampledBy(emptyEvent: ClientEvent[Int])
          .fold(10)(_ + _)
          .toDBehavior
        val result = dd.map2(count)(_ + _)

        val client = ClientDBehavior.toSession(result)

//        val emptyEvent = ClientEvent.sourceWithEngineEffect[Int] { (fire) =>
//          test = true
//        }
//        val count        = emptyEvent.fold(0)(_ + _).toDBehavior
//        val sessionCount = ClientDBehavior.toSession(count)
//
//        val previousPlayerPosition: ClientBehavior[Int] =
//          ClientDBehavior.delayed(position)
//
//        val direction =
//          ClientDBehavior.toSession(previousPlayerPosition.sampledBy(count))
//
//        val position: ClientDBehavior[Int] =
//          SessionDBehavior.toClient(direction.map2(sessionCount)(_ * _))

        assert(
          TestC.client.graph.replicationGraph.value
            .isInstanceOf[BehaviorClientToServer])

        @client val _ = {
          testAndReset(TestC.count.graph)(TestC)
          testAndReset(TestC.delayed.graph)(TestC)
          testAndReset(TestC.dd.graph)(TestC)
          testAndReset(TestC.result.graph)(TestC)
          testAndReset(TestC.client.graph)(TestC)
        }
      }
//      object TestS {
//        val emptyEvent = SessionEvent.empty[Int]
//        val count      = emptyEvent.fold(0)(_ + _).toDBehavior
//
//        val delayed: SessionBehavior[Int] =
//          SessionDBehavior.delayed(result)
//
//        val result = delayed.snapshotWith(count)(_ + _)
//
//        val client = SessionEvent.toClient(result.changes)
//      }
//
//      assert(TestS.result.underlying.initial === Map.empty)
//      assert(
//        TestS.client.graph.replicationGraph.value
//          .isInstanceOf[EventServerToClient])
    }
  }
}
