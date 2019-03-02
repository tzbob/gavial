package mtfrp.core
import scala.concurrent.Future

trait EventListener {
  def restart(url: String, handler: String => Unit): Unit
  val onFirstMessage: Future[Unit]
}
