package mtfrp.core

import hokko.{core => HC}
import slogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EventManager(graph: GraphState,
                   exitBehaviors: Seq[HC.CBehavior[_]],
                   exitEvents: Seq[HC.Event[_]])
    extends LazyLogging {
  val rgc = new ReplicationGraphClient(graph.replicationGraph.value)

  private val primitives = (rgc.exitEvent :: exitEvents.toList) ::: exitBehaviors.toList
  logger.debug(s"Compiling Engine for $primitives")
  val engine = HC.Engine.compile(primitives)

  def start(): Future[Unit] = {
    if (graph.requiresWebSockets.value) {
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
      Future {()}
    }
  }
}
