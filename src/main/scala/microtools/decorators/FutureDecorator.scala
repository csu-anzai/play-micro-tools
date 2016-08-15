package microtools.decorators

import scala.concurrent.Future


trait FutureDecorator[T] extends ((=> Future[T]) => Future[T]) {
  def andThen(other : FutureDecorator[T]) : FutureDecorator[T] = new CombinedFutureDecorator[T](this, other)
}

class CombinedFutureDecorator[T](first: FutureDecorator[T], second: FutureDecorator[T]) extends FutureDecorator[T] {
  override def apply(block: => Future[T]): Future[T] = first.apply(second.apply(block))
}