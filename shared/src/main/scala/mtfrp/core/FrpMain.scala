package mtfrp.core

trait FrpMain {
  def ui: ClientDBehavior[UI.HTML]

  def noWebSockets(
      interface: ClientDBehavior[UI.HTML]): ClientDBehavior[UI.HTML] = {
    if (interface.requiresWebSockets)
      throw new RuntimeException(
        """
           Your application requires web sockets. This typically means that
           you converted (indirectly) from the application layer to the
           client layer without sampling through an event with operations
           such as snapshot.
        """.stripMargin)
    interface
  }
}
