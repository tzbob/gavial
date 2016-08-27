package mtfrp.core

import java.util.concurrent.atomic.AtomicInteger

object HasToken {
  // FIXME: global state -- messed up test before!
  private[this] var counter       = new AtomicInteger(0)
  private def createNumber(): Int = counter.incrementAndGet()
  private[core] def reset(): Unit = counter = new AtomicInteger(0)
}

trait HasToken {
  val token = HasToken.createNumber()
}
