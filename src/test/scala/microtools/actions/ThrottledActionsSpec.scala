package microtools.actions

import java.time.Duration

import akka.actor.ActorSystem
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.components.OneAppPerSuiteWithComponents
import play.api.{BuiltInComponentsFromContext, Configuration}
import play.api.http.Status
import play.api.mvc.{AbstractController, EssentialFilter, Results}
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.reflectiveCalls

class ThrottledActionsSpec
    extends PlaySpec
    with MockitoSugar
    with ScalaFutures
    with OptionValues
    with OneAppPerSuiteWithComponents {
  override lazy val components: BuiltInComponentsFromContext =
    new BuiltInComponentsFromContext(context) {
      override def router: Router = Router.empty

      override def httpFilters: Seq[EssentialFilter] = Seq.empty
    }

  trait WithThrottled {}

  "Throttle" should {
    implicit val system: ActorSystem = mock[ActorSystem]
    val dao: RateCounter             = mock[RateCounter]

    val classUnderTest = new AbstractController(components.controllerComponents)
    with ThrottledActions {
      override def rateCounter: RateCounter = dao

      def throttled =
        ThrottledAction("test", Duration.ofMinutes(1), 5, 10).apply {
          Results.Ok("Ok")
        }
    }

    "throttle" should {

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
        when(dao.incrementAndGet(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("An error that should be handled")))

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
