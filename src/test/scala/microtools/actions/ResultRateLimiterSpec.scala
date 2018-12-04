package microtools.actions

import com.codahale.metrics.Counter
import java.time.Duration

import akka.actor.ActorSystem
import microtools.logging.LoggingContext
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status
import play.api.mvc._
import Results.NoContent
import play.api.test.Helpers._

import scala.concurrent.Future

class ResultRateLimiterSpec
    extends PlaySpec
    with MockitoSugar
    with BeforeAndAfterAll
    with ScalaFutures {

  implicit val system: ActorSystem = ActorSystem()

  override def afterAll {
    system.terminate()
  }

  "Throttle" should {
    implicit val ctx: LoggingContext = LoggingContext.static()
    val dao: RateCounter             = mock[RateCounter]

    val rateLimiter = new ResultRateLimiter(dao,
                                            mock[Counter],
                                            mock[Counter],
                                            Duration.ofMinutes(1),
                                            5,
                                            10,
                                            Duration.ofSeconds(1))

    "rate limiter" should {

      "return ok as long threshold isn't reached" in {
        when(dao.incrementAndGet(any(), any()))
          .thenReturn(Future.successful(4))

        status(rateLimiter.limit("test")(Future.successful(NoContent))) mustBe Status.NO_CONTENT
      }

      "return a 429 Too Many Requests (RFC 6585) if the hard threshold is reached" in {
        when(dao.incrementAndGet(any(), any()))
          .thenReturn(Future.successful(11))

        status(rateLimiter.limit("test")(Future.successful(NoContent))) mustBe Status.TOO_MANY_REQUESTS
      }

      "return none if the dao returns an failure" in {
        when(dao.incrementAndGet(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("An error that should be handled")))

        status(rateLimiter.limit("test")(Future.successful(NoContent))) mustBe Status.NO_CONTENT
      }
    }

  }
}
