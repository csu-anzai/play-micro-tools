package microtools.actions

import com.codahale.metrics.Counter
import java.time.Duration
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.after
import microtools.logging.{LoggingContext, WithContextAwareLogger}
import play.api.mvc._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class ResultRateLimiter(rateCounter: RateCounter,
                        softCounter: Counter,
                        hardCounter: Counter,
                        rateWindow: Duration,
                        softLimit: Long,
                        hardLimit: Long,
                        throttle: Duration)(implicit system: ActorSystem)
    extends WithContextAwareLogger {

  private implicit val ec: ExecutionContext = system.dispatcher

  def limit(limitName: String)(block: => Future[Result])(
      implicit ctx: LoggingContext): Future[Result] = {
    rateCounter
      .incrementAndGet(limitName, rateWindow)
      .recover {
        case e: Throwable =>
          log.error(s"Failed to get rate counter for $limitName", e)
          0
      }
      .flatMap {
        case counter if counter > hardLimit =>
          log.warn(s"Hard rate limit reached for $limitName")
          hardCounter.inc()
          Future.successful(Results.TooManyRequests)
        case counter if counter > softLimit =>
          softCounter.inc()
          log.info(s"Soft rate limit reached for $limitName. Throttle down.")
          after(
            FiniteDuration(
              throttle.toMillis,
              TimeUnit.MILLISECONDS
            ),
            system.scheduler
          ) {
            block
          }
        case _ =>
          block
      }
  }

}
