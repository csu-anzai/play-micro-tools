package microtools.actions

import microtools.logging.WithContextAwareLogger
import microtools.models.BasicAuthCredentials
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.components.OneAppPerSuiteWithComponents
import play.api.BuiltInComponentsFromContext
import play.api.http.Status
import play.api.mvc.{AbstractController, AnyContentAsEmpty, EssentialFilter}
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.reflectiveCalls

class BasicAuthActionSpec
    extends PlaySpec
    with MockitoSugar
    with ScalaFutures
    with OptionValues
    with WithContextAwareLogger
    with OneAppPerSuiteWithComponents {

  override lazy val components: BuiltInComponentsFromContext =
    new BuiltInComponentsFromContext(context) {
      override def router: Router = Router.empty

      override def httpFilters: Seq[EssentialFilter] = Seq.empty
    }

  trait WithBasicAuthAction {
    val credentials = BasicAuthCredentials("validUser", "validPass")

    val basicAuthAction = new AbstractController(components.controllerComponents)
    with AuthActions {
      def action = BasicAuthAction(credentials).async { implicit request =>
        log.info("onhreybirfgurfzryybspbpxvagurzbeavat")
        Future.successful(Ok)
      }
    }

    def validBasicAuthRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest()
        .withHeaders("Authorization" -> s"Basic ${credentials.asBase64String}")

    def unauthorizedBasicAuthRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest()
        .withHeaders(
          "Authorization" -> s"Basic ${BasicAuthCredentials("hans", "wurst").asBase64String}")

    def invalidBasicAuthRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest()
        .withHeaders("Authorization" -> "Bearer blubb")
  }

  "A basic auth action" should {
    "allow requests with valid header and credentials" in new WithBasicAuthAction {
      status(basicAuthAction.action(validBasicAuthRequest)) mustBe Status.OK
    }

    "deny requests with valid header and but invalid credentials" in new WithBasicAuthAction {
      status(basicAuthAction.action(unauthorizedBasicAuthRequest)) mustBe Status.UNAUTHORIZED
    }

    "deny requests with invalid header" in new WithBasicAuthAction {
      status(basicAuthAction.action(invalidBasicAuthRequest)) mustBe Status.UNAUTHORIZED
    }
  }
}
