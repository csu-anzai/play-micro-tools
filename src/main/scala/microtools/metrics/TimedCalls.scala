package microtools.metrics

import java.time.Instant

import com.codahale.metrics.{MetricRegistry, Timer}
import microtools.decorators.{FutureDecorator, TryDecorator}
import microtools.logging.{ContextAwareLogger, LoggingContext}
import microtools.{BusinessFailure, BusinessSuccess, BusinessTry}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait TimedCalls {
  def log: ContextAwareLogger

  def metricRegistry: MetricRegistry

  def timeFuture[T](callId: String)(implicit ec: ExecutionContext,
                                    ctx: LoggingContext): FutureDecorator[T] = {
    val timer = metricRegistry.timer(s"${log.name}.$callId")

    timeFuture(callId, timer)
  }

  def timeFuture[T](callId: String, timer: Timer)(implicit ec: ExecutionContext,
                                                  ctx: LoggingContext): FutureDecorator[T] =
    new FutureDecorator[T] {
      override def apply(block: => Future[T]): Future[T] = {
        val timeCtx = timer.time()
        val start   = Instant.now()
        val result  = block

        result.onComplete {
          case Success(_) =>
            val nanos = timeCtx.stop()
            log.info(s"$callId: Success", "millis" -> (nanos / 1000000.0).toString)
          case Failure(e) =>
            val nanos = timeCtx.stop()
            log.error(s"$callId: Internal error", e, "millis" -> (nanos / 1000000.0).toString)
        }

        result
      }
    }

  def timeTry[T](callId: String)(implicit ec: ExecutionContext,
                                 ctx: LoggingContext): TryDecorator[T] = {
    val timer = metricRegistry.timer(s"${log.name}.$callId")

    timeTry(callId, timer)
  }

  def timeTry[T](callId: String, timer: Timer)(implicit ec: ExecutionContext,
                                               ctx: LoggingContext): TryDecorator[T] =
    new TryDecorator[T] {
      override def apply(block: => BusinessTry[T]): BusinessTry[T] = {
        val timeCtx = timer.time()
        val start   = Instant.now()
        val result  = block

        result.onComplete {
          case Success(BusinessSuccess(_)) =>
            val nanos = timeCtx.stop()
            log.info(s"$callId: Success", "millis" -> (nanos / 1000000.0).toString)
          case Success(BusinessFailure(problem)) =>
            val nanos = timeCtx.stop()
            log.info(s"$callId: Problem: $problem", "millis" -> (nanos / 1000000.0).toString)
          case Failure(e) =>
            val nanos = timeCtx.stop()
            log.error(s"$callId: Internal error", e, "millis" -> (nanos / 1000000.0).toString)
        }
      }
    }
}
