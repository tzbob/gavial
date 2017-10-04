package mtfrp.core

import org.scalatest.{Matchers, WordSpec}

class ClientIncBehaviorTest extends WordSpec with Matchers {

  "ClientIncBehavior" must {
    "have initial values when replicated" in {
      val emptyEvent = AppEvent.empty[Client => Option[Int]]
      val zeroBehavior = emptyEvent.fold((c: Client) => 0) { (accF, _) =>
        accF
      }

      assert(AppIncBehavior.toClient(zeroBehavior).initial === 0)
    }
  }

}
