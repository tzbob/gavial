package mtfrp.core

trait EventListener {
  def restart(url: String, handler: String => Unit): Unit
}
