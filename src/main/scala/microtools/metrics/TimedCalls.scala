package microtools.metrics

import java.time.Instant

import com.codahale.metrics.{MetricRegistry, Timer}
import microtools.{BusinessFailure, BusinessSuccess, BusinessTry}
import microtools.logging.{LoggingContext, WithContextAwareLogger}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait TimedCalls extends WithContextAwareLogger {
  def metricRegistry: MetricRegistry

  def timedAsync[T](callId: String)(block: => Future[T])(
    implicit ec: ExecutionContext, ctx: LoggingContext): Future[T] = {
    val timer = metricRegistry.timer(s"${log.name}.$callId")

    timedAsync(callId, timer)(block)
  }

  def timedAsync[T](callId: String, timer: Timer)(block: => Future[T])(
    implicit ec: ExecutionContext, ctx: LoggingContext): Future[T] = {
    val timeCtx = timer.time()
    val start = Instant.now()
    val result = block

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

  def timedTry[T](callId:String)(block : => BusinessTry[T])(
    implicit ec: ExecutionContext, ctx: LoggingContext): BusinessTry[T] = {
    val timer = metricRegistry.timer(s"${log.name}.$callId")

    timedTry(callId, timer)(block)
  }

  def timedTry[T](callId: String, timer: Timer)(block: => BusinessTry[T])(
    implicit ec: ExecutionContext, ctx: LoggingContext): BusinessTry[T] = {

    val timeCtx = timer.time()
    val start = Instant.now()
    val result = block

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
