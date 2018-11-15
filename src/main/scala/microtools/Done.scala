package microtools

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

/**
  * Signal completion but there is no actual value completed. More clearly signals intent
  * than `Unit` and works around type safety issues related to Unit (value discard)
  * Inspired by Akka Done.
  */
sealed abstract class Done extends Serializable

case object Done extends Done {

  implicit class FutureDoneConverter[T](future: Future[T]) {
    def done(implicit ec: ExecutionContext): Future[Done] = future.map(_ => Done)
  }

  implicit class BusinessTryDoneConverter[T](future: BusinessTry[T]) {
    def done(implicit ec: ExecutionContext): BusinessTry[Done] = future.map(_ => Done)
  }
}
