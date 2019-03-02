package mtfrp.core

import org.scalajs.dom.{EventSource, MessageEvent}

import scala.concurrent.{Future, Promise}

class SseEventListener extends EventListener {
  private[this] var eventSource: EventSource = null
  def stop(): Unit =
    if (eventSource != null) eventSource.close()

  val p                            = Promise[Unit]()
  val onFirstMessage: Future[Unit] = p.future

  override def restart(url: String, handler: String => Unit): Unit = {
    stop()

    eventSource = new EventSource(url)
    val listener = { (m: MessageEvent) =>
      if (!p.isCompleted) p.success(())
      val data = m.data.asInstanceOf[String]
      handler(data)
    }
    eventSource.addEventListener("message", listener)
  }
}
