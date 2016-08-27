package mtfrp.core

import java.util.UUID

import scala.scalajs.js
import hokko.{core => HC}
import org.scalajs.dom.raw.{EventSource, MessageEvent}

trait MyMain extends js.JSApp with FrpMain {
  def main(): Unit = {
    val clientId = UUID.randomUUID()
    val client   = Client(clientId)

    val engine = HC.Engine.compile(Nil, Seq(ui.rep))
    val listener = new EventListener {
      private[this] var eventSource: EventSource = null
      def stop() =
        if (eventSource != null) eventSource.close()

      override def restart(url: String,
                           handlers: Map[String, (String) => Unit]): Unit = {
        stop()

        eventSource = new EventSource(url)
        handlers.foreach {
          case (msg, handler) =>
            eventSource.addEventListener(
              msg,
              (_: MessageEvent).data.asInstanceOf[String])
        }
      }
    }

    val receiver = new EventReceiver(ui.graph, engine, listener)
    receiver.restart(s"/events/$clientId")

    val currentUi = engine.askCurrentValues()(ui.rep)
    println(currentUi.get)
  }
}
