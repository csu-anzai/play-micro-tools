package microtools.actions

import java.time.Duration

import akka.actor.ActorSystem
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status
import play.api.mvc.{Action, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ThrottledActionsSpec
    extends PlaySpec
    with MockitoSugar
    with ScalaFutures
    with OptionValues {

  "Throttle" should {
    implicit val system: ActorSystem = mock[ActorSystem]
    val dao: RateCounter             = mock[RateCounter]

    val classUnderTest = new ThrottledActions {
      override def rateCounter: RateCounter = dao

      def throttled =
        ThrottledAction("test", Duration.ofMinutes(1), 5, 10).apply {
          Results.Ok("Ok")
        }
    }

    "throttle" should {
      val rootConfig     = mock[Configuration]
      val throttleConfig = mock[Configuration]

      when(rootConfig.getConfig(any())).thenReturn(Some(throttleConfig))
      when(throttleConfig.getInt("calls")).thenReturn(Some(10))
      when(throttleConfig.getInt("everyNSeconds")).thenReturn(Some(10))

      "return ok as long threshold isn't reached" in {
        when(dao.incrementAndGet(any(), any()))
          .thenReturn(Future.successful(4))

        val request = FakeRequest()
        status(classUnderTest.throttled(request)) mustBe Status.OK
      }

      "return a 429 Too Many Requests (RFC 6585) if the hard threshold is reached" in {
        when(dao.incrementAndGet(any(), any()))
          .thenReturn(Future.successful(11))

        val request = FakeRequest()
        status(classUnderTest.throttled(request)) mustBe Status.TOO_MANY_REQUESTS
      }

      "return none if the dao returns an failure" in {
        when(dao.incrementAndGet(any(), any())).thenReturn(Future.failed(
                new RuntimeException("An error that should be handled")))

        val request = FakeRequest()
        status(classUnderTest.throttled(request)) mustBe Status.OK
      }
    }

    "ipKey" should {
      "generate the pattern <key>-<remoteAddress>" in {
        val request =
          FakeRequest().withHeaders("X-Forwarded-For" -> "123.123.123.123")

        classUnderTest.requestIP(request) must be("123.123.123.123")
      }
    }
  }
}
