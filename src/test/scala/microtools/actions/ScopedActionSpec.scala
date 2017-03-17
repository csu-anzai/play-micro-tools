package microtools.actions

import microtools.models.ExtraHeaders
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.reflectiveCalls

class ScopedActionSpec extends PlaySpec with MockitoSugar with ScalaFutures with OptionValues {

  trait WithScopedAction {

    implicit val serviceName = ServiceName("scopedActionSpec")
    val scopeRequirement     = ScopeRequirement.require("W") or ScopeRequirement.require("R")
    val scopedAction = new AuthActions {
      def action = (AuthAction andThen ScopedAction(scopeRequirement)) { request =>
        Results.NoContent
      }
    }

    def authorizedRequest =
      FakeRequest()
        .withHeaders(ExtraHeaders.AUTH_SUBJECT_HEADER -> "", ExtraHeaders.AUTH_TOKEN_HEADER -> "")
  }
  "A scoped action" should {
    "allow requests with exact scope match" in new WithScopedAction {

      val requestWithW = authorizedRequest.withHeaders(s"X-Auth-Scopes-${serviceName.name}" -> "W",
                                                       s"X-Auth-Scopes-${serviceName.name}" -> "X")
      status(scopedAction.action(requestWithW)) mustBe Status.NO_CONTENT

      val requestWithR = authorizedRequest.withHeaders(s"X-Auth-Scopes-${serviceName.name}" -> "R",
                                                       s"X-Auth-Scopes-${serviceName.name}" -> "X")
      status(scopedAction.action(requestWithR)) mustBe Status.NO_CONTENT
    }

    "allow requests with wildcard scope match" in new WithScopedAction {

      val request = authorizedRequest.withHeaders(s"X-Auth-Scopes-${serviceName.name}" -> "*")
      status(scopedAction.action(request)) mustBe Status.NO_CONTENT
    }

    "forbid requests with wrong scopes" in new WithScopedAction {

      val request = authorizedRequest.withHeaders(s"X-Auth-Scopes-${serviceName.name}" -> "U",
                                                  s"X-Auth-Scopes-${serviceName.name}" -> "X")
      status(scopedAction.action(request)) mustBe Status.FORBIDDEN
    }
    "forbid requests with wildcard scope for other service" in new WithScopedAction {

      val request = authorizedRequest.withHeaders(s"X-Auth-Scopes-Bauer" -> "*")
      status(scopedAction.action(request)) mustBe Status.FORBIDDEN
    }
  }
}
