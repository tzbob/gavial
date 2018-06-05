package mtfrp.core

import java.util.concurrent.atomic.AtomicInteger

object HasToken {
  // FIXME: global state -- messed up test before!
  // Makes parallel tests unpredictable
  private[this] var counter       = new AtomicInteger(0)
  private def createNumber(): Int = counter.incrementAndGet()
  private[core] def reset(): Unit = counter.set(0)
}

trait HasToken {
  val token = HasToken.createNumber()
}
