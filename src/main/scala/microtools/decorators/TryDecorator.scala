package microtools.decorators

import microtools.BusinessTry

trait TryDecorator[T] extends ((=> BusinessTry[T]) => BusinessTry[T]) {
  def andThen(other: TryDecorator[T]): TryDecorator[T] = new CombinedTryDecorator[T](this, other)
}

class CombinedTryDecorator[T](first: TryDecorator[T], second: TryDecorator[T]) extends TryDecorator[T] {
  override def apply(block: => BusinessTry[T]): BusinessTry[T] = first.apply(second.apply(block))
}