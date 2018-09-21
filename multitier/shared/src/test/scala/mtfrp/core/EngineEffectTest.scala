package mtfrp.core

import cats.Eval
import hokko.core.Engine
import org.scalatest.WordSpec

class EngineEffectTest extends WordSpec {

  "GraphState" should {

    "carry engine effects through merges" in {
      var test = false

      val effed = GraphState.default.withEffect(Eval.later { (e: Engine) =>
        test = true
      })

      val res = GraphState.default.mergeGraphAndEffect(effed)

      assert(!test)
      res.effect.value(null)

      assert(test)
    }

    "carry engine effects through monoid composition" in {
      var test = false

      val effed = GraphState.default.withEffect(Eval.later { (e: Engine) =>
        test = true
      })

      val res = GraphState.any.combine(effed, GraphState.default)

      assert(!test)
      res.effect.value(null)
      assert(test)
    }
  }
}
