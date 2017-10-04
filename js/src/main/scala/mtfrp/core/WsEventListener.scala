package mtfrp.core

import org.scalajs.dom.{MessageEvent, WebSocket}
import slogging.LazyLogging

class WsEventListener(onOpen: WebSocket => Unit)
    extends EventListener
    with LazyLogging {
  private[this] var websocket: WebSocket = null
  def stop(): Unit =
    if (websocket != null) websocket.close()

  override def restart(url: String, handler: String => Unit): Unit = {
    stop()

    websocket = new WebSocket(url)
    websocket.addEventListener("open", { _: Any =>
      onOpen(websocket)
    })

    val listener = { (m: MessageEvent) =>
      val jsonData = m.data.asInstanceOf[String]
      handler(jsonData)
    }
    websocket.addEventListener("message", listener)
  }
}
