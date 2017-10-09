package mtfrp.core

trait FrpMain {
  def ui: ClientDBehavior[UI.HTML]
}
