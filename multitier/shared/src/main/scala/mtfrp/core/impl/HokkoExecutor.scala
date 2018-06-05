package mtfrp.core.impl

import scala.concurrent.Future

class HokkoExecutor[T <: HokkoTier: HokkoBuilder] {

  def async[A](future: HokkoEvent[T, Future[A]]): HokkoEvent[T, A] = ???

}
