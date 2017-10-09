package mtfrp.core

trait FrpMain {
  def ui: ClientDiscreteBehavior[UI.HTML]
}
