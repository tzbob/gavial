package mtfrp.core

import hokko.{core => HC}

class EventManager(graph: ReplicationGraph,
                   exitBehaviors: Seq[HC.CBehavior[_]],
                   exitEvents: Seq[HC.Event[_]]) {
  val rgc = new ReplicationGraphClient(graph)
  val engine =
    HC.Engine.compile(
      (rgc.exitEvent :: exitEvents.toList) ::: exitBehaviors.toList)

  val receiver = new EventReceiver(rgc, engine, new SseEventListener)
  val sender   = new EventSender(rgc, engine)

  def startSending(url: String): Unit   = sender.start(url)
  def startReceiving(url: String): Unit = receiver.restart(url)
}
