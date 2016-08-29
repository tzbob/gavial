package mtfrp.core

import java.util.UUID

object ClientGenerator {
  private[core] val static: Client = Client(UUID.randomUUID())
  private[core] def fresh: Client = Client(UUID.randomUUID())
}
