package mtfrp.core

import mtfrp.core.ReplicationGraph.{EventClientToServer, EventServerToClient}
import org.scalatest.WordSpec

class DBehaviorTest extends WordSpec {

  "DBehaviors" can {

    "be defined recursively" in {

      object TestA {
        val emptyEvent = AppEvent.source[Int]
        val count      = emptyEvent.fold(0)(_ + _).toDBehavior

        val delayed: AppDBehavior[Int] = AppDBehavior.delayed(result, 200)
        val result = count.map2(delayed)(_ + _)

        val client = SessionEvent.toClient(AppEvent.toSession(result.changes))
      }

      assert(TestA.result.initial === 200)
      assert(
        TestA.client.graph.replicationGraph.isInstanceOf[EventServerToClient])

      object TestC {
        val emptyEvent = ClientEvent.source[Int]
        val count      = emptyEvent.fold(0)(_ + _).toDBehavior

        val delayed: ClientDBehavior[Int] = ClientDBehavior.delayed(result, 200)

        val result = count.map2(delayed)(_ + _)

        val sess = ClientEvent.toSession(result.changes)
      }

      assert(TestC.result.initial === 200)
      assert(
        TestC.sess.graph.replicationGraph.isInstanceOf[EventClientToServer])

      object TestS {
        val emptyEvent = SessionEvent.empty[Int]
        val count      = emptyEvent.fold(0)(_ + _).toDBehavior

        val delayed: SessionDBehavior[Int] =
          SessionDBehavior.delayed(result, 200)

        val result = count.map2(delayed)(_ + _)

        val client = SessionEvent.toClient(result.changes)
      }

      assert(TestS.result.underlying.initial === Map.empty)
      assert(
        TestS.client.graph.replicationGraph.isInstanceOf[EventServerToClient])
    }
  }
}
