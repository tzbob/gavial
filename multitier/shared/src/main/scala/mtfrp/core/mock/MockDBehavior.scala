package mtfrp.core.mock

import hokko.core.Thunk
import mtfrp.core._

class MockDBehavior[T <: MockTier: MockBuilder, A](
    graphByName: => GraphState,
    initialByName: => A
) extends DBehavior[T, A] {
  private[core] lazy val graph   = graphByName
  private[core] lazy val initial = initialByName

  private[this] val mockBuilder = implicitly[MockBuilder[T]]

  def changes(): T#Event[A] =
    mockBuilder.event(graph)

  def reverseApply[B](fb: T#DBehavior[A => B]): T#DBehavior[B] =
    mockBuilder.DBehavior(GraphState.any.combine(graph, fb.graph),
                          fb.initial(initial))

  def snapshotWith[B, C](ev: T#Event[B])(f: (A, B) => C): T#Event[C] =
    mockBuilder.event(ev.graph.mergeGraphAndEffect(this.graph))

  def toBehavior: T#Behavior[A] =
    mockBuilder.behavior(graph, Thunk.eager(initial))

  def toIBehavior[DeltaA](diff: (A, A) => DeltaA)(
      patch: (A, DeltaA) => A): T#IBehavior[A, DeltaA] =
    mockBuilder.IBehavior(graph, patch, initial)
}

abstract class MockDBehaviorObject[
    SubT <: MockTier { type T = SubT }: MockBuilder]
    extends DBehaviorObject[SubT] {
  private[this] val mockBuilder = implicitly[MockBuilder[SubT]]

  def constant[A](x: A): SubT#DBehavior[A] =
    mockBuilder.DBehavior(GraphState.default, x)

  def delayed[A](db: => SubT#DBehavior[A]): SubT#Behavior[A] =
    mockBuilder.behavior(GraphState.delayed(db.graph), Thunk(db.initial))
}
