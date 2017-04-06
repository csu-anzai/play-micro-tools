package microtools.actions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.reflectiveCalls

import microtools.{BusinessCondition, BusinessTry}
import microtools.models.{ExtraHeaders, ServiceName}
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers._

class ScopedActionSpec extends PlaySpec with MockitoSugar with ScalaFutures with OptionValues {

  trait WithScopedAction {

    implicit val serviceName = ServiceName("scopedActionSpec")
    val scopeRequirement     = StandardScopeRequirements.read or StandardScopeRequirements.write
    val scopedAction = new AuthActions {
      def action = (AuthAction andThen ScopedAction(scopeRequirement)).async { implicit request =>
        val condition: BusinessCondition[String] = scopeRequirement
        condition("Test").asResult
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
      status(scopedAction.action(requestWithW)) mustBe Status.OK

      val requestWithR = authorizedRequest.withHeaders(s"X-Auth-Scopes-${serviceName.name}" -> "R",
                                                       s"X-Auth-Scopes-${serviceName.name}" -> "X")
      status(scopedAction.action(requestWithR)) mustBe Status.OK
    }

    "allow requests with wildcard scope match" in new WithScopedAction {

      val request = authorizedRequest.withHeaders(s"X-Auth-Scopes-${serviceName.name}" -> "*")
      status(scopedAction.action(request)) mustBe Status.OK
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

  "A ScopeRequest" should {
    "by convertable to a Business condition" in new WithScopedAction {
      val requestWithW = authorizedRequest.withHeaders(s"X-Auth-Scopes-${serviceName.name}" -> "W",
                                                       s"X-Auth-Scopes-${serviceName.name}" -> "X")
      val requestWithR = authorizedRequest.withHeaders(s"X-Auth-Scopes-${serviceName.name}" -> "R",
                                                       s"X-Auth-Scopes-${serviceName.name}" -> "X")

      val conditionalAction = new AuthActions {
        def action = (AuthAction andThen ScopedAction(scopeRequirement)).async {
          implicit request =>
            val valueTry = BusinessTry.success("The value")

            (for {
              value <- valueTry.withCondition(StandardScopeRequirements.write)
            } yield Results.Ok(value)).asResult
        }
      }

      status(conditionalAction.action(requestWithW)) mustBe Status.OK
      status(conditionalAction.action(requestWithR)) mustBe Status.FORBIDDEN

    }
  }
}
