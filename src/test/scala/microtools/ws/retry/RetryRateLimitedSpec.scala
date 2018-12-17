package microtools.ws.retry
import akka.actor.ActorSystem
import microtools.logging.LoggingContext
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

class RetryRateLimitedSpec
    extends FlatSpec
    with Matchers
    with ScalaFutures
    with MockitoSugar
    with BeforeAndAfterAll {

  implicit override val patienceConfig =
    PatienceConfig(timeout = Span(30, Seconds), interval = Span(20, Millis))

  implicit val loggingContext = LoggingContext.static()

  implicit val as        = ActorSystem("test")
  implicit val ec        = as.dispatcher
  implicit val scheduler = as.scheduler

  override def afterAll() {
    as.terminate().futureValue
  }

  it should "abort after max retries" in {

    val response = mock[WSResponse]
    when(response.status).thenReturn(429)
    when(response.headerValues("Retry-After")).thenReturn(Seq("1"))

    val r = RetryRateLimited(
      Future.successful(response),
      maxRetries = 2
    )

    r.futureValue.status shouldBe 429
  }

  it should "successfully retry" in {

    val response = mock[WSResponse]
    when(response.status).thenReturn(429).thenReturn(200)
    when(response.headerValues("Retry-After")).thenReturn(Seq("1"))

    val r = RetryRateLimited(
      Future.successful(response),
      maxRetries = 2
    )

    r.futureValue.status shouldBe 200
  }
}
