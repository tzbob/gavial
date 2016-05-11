package mtfrp.core

import java.util.concurrent.atomic.AtomicInteger
import hokko.core

object HasToken {
  private[this] var counter = new AtomicInteger(0)
  private def createNumber(): Int = counter.incrementAndGet()
}

trait HasToken {
  val id = HasToken.createNumber()
}
