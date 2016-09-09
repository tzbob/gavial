package mtfrp.core

import java.util.UUID

object ClientGenerator {
  // FIXME: Global state
  private[core] val static: Client = Client(UUID.randomUUID())
  private[core] def fresh: Client  = Client(UUID.randomUUID())
}
