package mtfrp.core

trait EventListener {
  def restart(url: String, handlers: Map[String, String => Unit]): Unit
}
