package mtfrp.core

import org.scalajs.dom.{EventSource, MessageEvent}

class SseEventListener extends EventListener {
  private[this] var eventSource: EventSource = null
  def stop(): Unit =
    if (eventSource != null) eventSource.close()

  override def restart(url: String,
                       handlers: Map[String, (String) => Unit]): Unit = {
    stop()

    eventSource = new EventSource(url)
    handlers.foreach {
      case (msg, handler) =>
        val listener = { (m: MessageEvent) =>
          val data = m.data.asInstanceOf[String]
          handler(data)
        }
        eventSource.addEventListener(msg, listener)
    }
  }
}
