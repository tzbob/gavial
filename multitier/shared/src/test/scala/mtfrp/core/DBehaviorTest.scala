package mtfrp.core

import mtfrp.core.ReplicationGraph.{BehaviorServerToClient, EventClientToServer, EventServerToClient}
import org.scalatest.WordSpec

class DBehaviorTest extends WordSpec {

  "DBehaviors" can {

    "be defined recursively" in {

      object TestA {
        val emptyEvent = AppEvent.source[Int]
        val count      = emptyEvent.fold(0)(_ + _).toDBehavior

        val delayed: AppDBehavior[Int] = AppDBehavior.delayed(result, 200)
        val result                     = count.map2(delayed)(_ + _)

        val client = SessionEvent.toClient(AppEvent.toSession(result.changes))
      }

      assert(TestA.result.initial === 200)
      assert(
        TestA.client.graph.replicationGraph.value
          .isInstanceOf[EventServerToClient])

      object TestC {
        val previousPlayerPosition: ClientDBehavior[Int] =
          ClientDBehavior.delayed(position, 0)

        val pickRight: (Int, Int) => Int = (_, a) => a

        val direction =
          ClientIBehavior
            .toSession(previousPlayerPosition.toIBehavior(pickRight)(pickRight))
            .toDBehavior

        val position: ClientDBehavior[Int] =
          SessionDBehavior.toClient(direction.map(_ * 3))
      }

      assert(
        TestC.position.graph.replicationGraph.value
          .isInstanceOf[BehaviorServerToClient])

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
        TestS.client.graph.replicationGraph.value
          .isInstanceOf[EventServerToClient])
    }
  }
}
