package mtfrp.core

import org.scalatest.{Matchers, WordSpec}

class ClientIBehaviorTest extends WordSpec with Matchers {

  "ClientIBehavior" must {
    "have initial values when replicated" in {
      val emptyEvent = AppEvent.empty[Client => Option[Int]]
      val zeroBehavior = emptyEvent.fold((c: Client) => 0) { (accF, _) =>
        accF
      }

      assert(AppIBehavior.toClient(zeroBehavior, 0).initial === 0)
    }
  }

}
