package mtfrp.core

import hokko.{core => HC}
import slogging.LazyLogging

class EventManager(requiresWebSocket: Boolean,
                   graph: ReplicationGraph,
                   exitBehaviors: Seq[HC.CBehavior[_]],
                   exitEvents: Seq[HC.Event[_]])
    extends LazyLogging {
  val rgc = new ReplicationGraphClient(graph)

  private val primitives = (rgc.exitEvent :: exitEvents.toList) ::: exitBehaviors.toList
  logger.debug(s"Compiling Engine for $primitives")
  val engine = HC.Engine.compile(primitives)

  def start(): Unit = {
    if (requiresWebSocket) {
      println("Application requires Web Sockets.")
      val receiver = new EventReceiver(rgc, engine, new WsEventListener(ws => {
        val sender = new WsEventSender(rgc, engine, ws)
        sender.start()
      }))
      val url = s"/${Names.ws}/${ClientGenerator.static.id}"
      receiver.restart(url)
    } else {
      println("Application does not require Web Sockets, using XHRs.")
      val url = s"/${Names.xhr}/${ClientGenerator.static.id}"
      new XhrEventSender(rgc, engine).start(url)
    }
  }
}
