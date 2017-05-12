package microtools.decorators

import akka.actor.Scheduler
import akka.pattern.after
import microtools.BusinessTry
import microtools.logging.{ContextAwareLogger, LoggingContext}
import microtools.models.Problem

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait Retries {
  def log: ContextAwareLogger

  def retryFuture[T](
      maxRetries: Int,
      delay: FiniteDuration,
      errorHandler: PartialFunction[(Throwable, Int), Future[T]] = PartialFunction.empty
  )(implicit ec: ExecutionContext, ctx: LoggingContext, s: Scheduler): FutureDecorator[T] =
    new FutureDecorator[T] {
      override def apply(block: => Future[T]): Future[T] = {
        Retries.materialize(block).recoverWith {
          case e if errorHandler.isDefinedAt(e, maxRetries) =>
            errorHandler.apply(e, maxRetries)
          case e if maxRetries > 0 =>
            log.warn(s"Retrying on ${e.getMessage}")
            after(delay, s)(retryFuture(maxRetries - 1, delay, errorHandler).apply(block))
          case e: Throwable =>
            log.error("Retries exhausted", e)
            Future.failed(e)
        }
      }
    }

  def retryTry[T](
      maxRetries: Int,
      delay: FiniteDuration,
      problemHandler: PartialFunction[(Problem, Int), BusinessTry[T]] = PartialFunction.empty,
      errorHandler: PartialFunction[(Throwable, Int), BusinessTry[T]] = PartialFunction.empty
  )(
      implicit ec: ExecutionContext,
      ctx: LoggingContext,
      s: Scheduler
  ): TryDecorator[T] =
    new TryDecorator[T] {
      override def apply(block: => BusinessTry[T]): BusinessTry[T] = {
        BusinessTry.future(
          Retries
            .materialize(block.asFuture)
            .recoverWith {
              case e if errorHandler.isDefinedAt(e, maxRetries) =>
                errorHandler.apply(e, maxRetries).asFuture
              case e if maxRetries > 0 =>
                log.warn(s"Retrying on ${e.getMessage}")
                after(delay, s)(
                  retryTry(maxRetries - 1, delay, problemHandler, errorHandler)
                    .apply(block)
                    .asFuture
                )
              case e: Throwable =>
                log.error("Retries exhausted", e)
                Future.failed(e)
            }
            .map {
              case Left(success) => BusinessTry.success(success)
              case Right(problem) if problemHandler.isDefinedAt(problem, maxRetries) =>
                problemHandler.apply(problem, maxRetries)
              case Right(problem) => BusinessTry.failure(problem)
            }
        )
      }
    }
}

object Retries {
  def materialize[T](block: => Future[T]): Future[T] =
    try block
    catch { case NonFatal(t) => Future.failed(t) }
}
