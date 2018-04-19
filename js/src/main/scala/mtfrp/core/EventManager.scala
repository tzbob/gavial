package mtfrp.core

import hokko.core.Primitive
import hokko.{core => HC}
import slogging.LazyLogging

class EventManager(graph: ReplicationGraph,
                   exitBehaviors: Seq[HC.CBehavior[_]],
                   exitEvents: Seq[HC.Event[_]])
    extends LazyLogging {
  val rgc = new ReplicationGraphClient(graph)

  private val primitives = (rgc.exitEvent :: exitEvents.toList) ::: exitBehaviors.toList
  logger.debug(s"Compiling Engine for $primitives")
  val engine = HC.Engine.compile(primitives)

  def start(url: String): Unit = {
    val receiver = new EventReceiver(rgc, engine, new WsEventListener(ws => {
      val sender = new EventSender(rgc, engine, ws)
      sender.start()
    }))

    receiver.restart(url)
  }
}
