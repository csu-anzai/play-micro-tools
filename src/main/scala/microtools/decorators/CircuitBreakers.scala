package microtools.decorators

import akka.actor.Scheduler
import akka.pattern.{CircuitBreaker, CircuitBreakerOpenException}
import microtools.{BusinessTry, ProblemException}
import microtools.logging.{ContextAwareLogger, LoggingContext}
import microtools.models.Problems

import scala.concurrent.{ExecutionContext, Future}

trait CircuitBreakers {
  def log: ContextAwareLogger

  def circuitBreakFuture[T](
      callId: String,
      maxFailures: Int,
      callTimeout: java.time.Duration,
      resetTimeout: java.time.Duration
  )(implicit ctx: LoggingContext, s: Scheduler): FutureDecorator[T] = {
    val circuitBreaker =
      CircuitBreaker.create(s, maxFailures, callTimeout, resetTimeout)

    circuitBreaker.onOpen {
      log.warn(s"$callId: Circuit opened")
    }
    circuitBreaker.onClose {
      log.warn(s"$callId: Circuit closed")
    }

    new FutureDecorator[T] {
      override def apply(block: => Future[T]): Future[T] = {
        circuitBreaker.withCircuitBreaker(block)
      }
    }
  }

  def circuitBreakTry[T](
      callId: String,
      maxFailures: Int,
      callTimeout: java.time.Duration,
      resetTimeout: java.time.Duration
  )(implicit ec: ExecutionContext, ctx: LoggingContext, s: Scheduler): TryDecorator[T] = {
    val circuitBreaker =
      CircuitBreaker.create(s, maxFailures, callTimeout, resetTimeout)

    circuitBreaker.onOpen {
      log.warn(s"$callId: Circuit opened")
    }
    circuitBreaker.onClose {
      log.warn(s"$callId: Circuit closed")
    }

    new TryDecorator[T] {
      override def apply(block: => BusinessTry[T]): BusinessTry[T] = {
        BusinessTry.future(
          circuitBreaker
            .withCircuitBreaker(block.asFuture.flatMap {
              case Left(success) =>
                Future.successful(BusinessTry.success(success))
              case Right(problem) if problem.code >= 500 =>
                Future.failed(new ProblemException(problem))
              case Right(problem) =>
                Future.successful(BusinessTry.failure(problem))
            })
            .recover {
              case e: CircuitBreakerOpenException =>
                log.error(s"$callId: Circuit breaker open")
                BusinessTry.failure(
                  Problems.SERVICE_UNAVAILABLE.withDetails(s"$callId: Circuit breaker open")
                )
            }
        )
      }
    }
  }
}
