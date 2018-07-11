package mtfrp.core

import mtfrp.core.ReplicationGraph.EventServerToClient
import org.scalatest.WordSpec

class AppDBehaviorTest extends WordSpec {

  "AppDBehavior" can {
    "be defined recursively" in {

      object Test {
        val emptyEvent = AppEvent.source[Int]
        val count      = emptyEvent.fold(0)(_ + _).toDBehavior

        val delayed = AppDBehavior.delayed(count, 200)

        val result = count.map2(delayed)(_ + _)

        val client = SessionEvent.toClient(AppEvent.toSession(result.changes))
      }

      assert(Test.result.initial === 200)
      assert(
        Test.client.graph.replicationGraph
          .isInstanceOf[EventServerToClient])
    }
  }
}
