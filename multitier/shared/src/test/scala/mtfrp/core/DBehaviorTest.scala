package mtfrp.core

import mtfrp.core.ReplicationGraph.{
  BehaviorServerToClient,
  EventClientToServer,
  EventServerToClient
}
import org.scalatest.WordSpec
import scala.language.reflectiveCalls

class DBehaviorTest extends WordSpec {

  "DBehaviors" can {

    "be defined recursively" in {
      def testAndReset(a: GraphState)(testObj: { var test: Boolean }) = {
        assert(!testObj.test)
        a.effect.value(null)
        assert(testObj.test)
        testObj.test = false
      }

      object TestA {
        var test = false
        val emptyEvent = AppEvent.sourceWithEngineEffect[Int] { fire =>
          test = true
        }
        val count = emptyEvent.fold(0)(_ + _).toDBehavior

        val delayed: AppDBehavior[Int] = AppDBehavior.delayed(result)
        val result                     = count.map2(delayed)(_ + _)

        val client = SessionEvent.toClient(AppEvent.toSession(result.changes))
      }

      assert(TestA.result.initial === 200)
      assert(
        TestA.client.graph.replicationGraph.value
          .isInstanceOf[EventServerToClient])

      testAndReset(TestA.emptyEvent.graph)(TestA)
      testAndReset(TestA.count.graph)(TestA)
      testAndReset(TestA.result.graph)(TestA)
      testAndReset(TestA.client.graph)(TestA)

      object TestC {
        var test = false
        val emptyEvent = ClientEvent.sourceWithEngineEffect[Int] { (fire) =>
          test = true
        }
        val count =
          ClientIBehavior.toSession(emptyEvent.fold(0)(_ + _)).toDBehavior

        val previousPlayerPosition: ClientDBehavior[Int] =
          ClientDBehavior.delayed(position)

        val pickRight: (Int, Int) => Int = (_, a) => a

        val direction =
          ClientIBehavior
            .toSession(previousPlayerPosition.toIBehavior(pickRight)(pickRight))
            .toDBehavior

        val position: ClientDBehavior[Int] =
          SessionDBehavior.toClient(direction.map2(count)(_ * _))
      }

      assert(
        TestC.position.graph.replicationGraph.value
          .isInstanceOf[BehaviorServerToClient])

      testAndReset(TestC.emptyEvent.graph)(TestC)
      testAndReset(TestC.count.graph)(TestC)
      testAndReset(TestC.position.graph)(TestC)

      object TestS {
        val emptyEvent = SessionEvent.empty[Int]
        val count      = emptyEvent.fold(0)(_ + _).toDBehavior

        val delayed: SessionDBehavior[Int] =
          SessionDBehavior.delayed(result)

        val result = count.map2(delayed)(_ + _)

        val client = SessionEvent.toClient(result.changes)
      }

      assert(TestS.result.underlying.initial === Map.empty)
      assert(
        TestS.client.graph.replicationGraph.value
          .isInstanceOf[EventServerToClient])
    }
  }
}
