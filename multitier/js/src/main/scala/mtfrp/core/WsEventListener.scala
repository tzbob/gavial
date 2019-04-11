package mtfrp.core

import org.scalajs.dom
import org.scalajs.dom.{MessageEvent, WebSocket}
import slogging.LazyLogging

import scala.concurrent.{Future, Promise}

class WsEventListener(onOpen: WebSocket => Unit)
    extends EventListener
    with LazyLogging {
  private[this] var websocket: WebSocket = null
  def stop(): Unit =
    if (websocket != null) websocket.close()

  val p: Promise[Unit]             = Promise[Unit]()
  val onFirstMessage: Future[Unit] = p.future

  override def restart(url: String, handler: String => Unit): Unit = {
    stop()

    val location = dom.window.location
    val proto =
      if (location.protocol == "https") "wss"
      else "ws"

    val rest = s"${location.host}${location.pathname}$url"
      .split('/')
      .filter(_ != "")
      .mkString("/")

    val wsUrl = s"$proto://$rest"

    websocket = new WebSocket(wsUrl)
    websocket.addEventListener("open", { _: Any =>
      logger.info(s"WebSocket opened on $wsUrl")
      onOpen(websocket)
    })

    val listener = { (m: MessageEvent) =>
      val jsonData = m.data.asInstanceOf[String]
      if (jsonData != "hb") {
        // ignore heartbeats
        logger.debug(s"Message retrieved from WebSocket: $jsonData")
        handler(jsonData)
        if (!p.isCompleted) p.success(())
      }
    }
    websocket.addEventListener("message", listener)
  }
}
