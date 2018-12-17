package microtools.ws.retry

import java.util.concurrent.{ThreadLocalRandom, TimeUnit}

import akka.actor.Scheduler
import akka.pattern.after
import microtools.logging.{LoggingContext, WithContextAwareLogger}
import play.api.http.Status
import play.api.libs.ws.WSResponse

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future}

object RetryRateLimited extends Status with WithContextAwareLogger {

  def apply(f: => Future[WSResponse],
            maxRetries: Int = 5,
            retry: Int = 0,
            startDelay: FiniteDuration = 1.second)(implicit executor: ExecutionContext,
                                                   scheduler: Scheduler,
                                                   ctx: LoggingContext): Future[WSResponse] = {
    def withRetry(f: => Future[WSResponse],
                  maxRetries: Int,
                  retry: Int,
                  startDelay: FiniteDuration): Future[WSResponse] = {
      f.flatMap { r =>
        {
          if (r.status == TOO_MANY_REQUESTS && retry < maxRetries) {
            val exponentialBackoffDelay = expBackOff(startDelay.toSeconds, 2, retry)
            val delayInSeconds = r
              .headerValues("Retry-After")
              .headOption
              .map(_.toLong)
              .getOrElse(exponentialBackoffDelay)

            val delayWithJitterMillis =
              Jitter.defaultRandom(delayInSeconds * 1000, delayInSeconds * 1000 * 2)
            log.info(
              s"Got a 429, schedule retry nr: ${retry + 1}, delay in ms: $delayWithJitterMillis\n")
            after(FiniteDuration(delayWithJitterMillis, TimeUnit.MILLISECONDS), scheduler)(
              withRetry(f, maxRetries, retry + 1, startDelay))
          } else {
            Future.successful(r)
          }
        }
      }
    }
    withRetry(f, maxRetries, retry, startDelay)
  }

  private def expBackOff(start: Long, base: Long, attempt: Long): Long = {
    val temp = start * math.pow(base, attempt)
    if (temp < 0.0 || temp > Long.MaxValue.toDouble) Long.MaxValue
    else temp.toLong
  }
}

object Jitter {

  import java.util.Random

  val defaultRandom: RandomSource = randomSource(ThreadLocalRandom.current())

  /** Given a lower and upper bound (inclusive) generate a random
    * number within those bounds */
  type RandomSource = (Long, Long) => Long

  /** Create a RandomSource from an instance of java.util.Random
    * Please be mindful of the call-by-name semantics */
  def randomSource(random: => Random): RandomSource = { (l, u) =>
    val (_l, _u) = if (l < u) (l, u) else (u, l)
    nextLong(random, (_u - _l) + 1) + _l
  }

  private def nextLong(random: Random, n: Long): Long = {
    if (n <= 0L) throw new IllegalArgumentException()

    // for small n use nextInt and cast
    if (n <= Int.MaxValue) random.nextInt(n.toInt).toLong
    else {
      // for large n use nextInt for both high and low ints
      val highLimit = (n >> 32).toInt
      val high      = random.nextInt(highLimit).toLong << 32
      val low       = random.nextInt().toLong & 0xffffffffL
      high | low
    }
  }
}
