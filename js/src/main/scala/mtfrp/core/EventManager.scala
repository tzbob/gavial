package mtfrp.core

import hokko.{core => HC}

class EventManager(graph: ReplicationGraph,
                   exitBehaviors: Seq[HC.CBehavior[_]],
                   exitEvents: Seq[HC.Event[_]]) {
  val rgc = new ReplicationGraphClient(graph)
  val engine =
    HC.Engine.compile(
      (rgc.exitEvent :: exitEvents.toList) ::: exitBehaviors.toList)

  def start(url: String): Unit = {
    val receiver = new EventReceiver(rgc, engine, new WsEventListener(ws => {
      val sender = new EventSender(rgc, engine, ws)
      sender.start()
    }))

    receiver.restart(url)
  }
}
