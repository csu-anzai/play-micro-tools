package microtools.actions

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
    val scopeRequirement     = ScopeRequirement.require("W") or ScopeRequirement.require("R")
    val scopedAction = ScopedAction(scopeRequirement).apply { request =>
      Results.NoContent
    }
  }
  "A scoped action" should {
    "allow requests with exact scope match" in new WithScopedAction {

      val requestWithW = FakeRequest().withHeaders(s"X-Auth-Scopes-$serviceName" -> "W",
                                                   s"X-Auth-Scopes-$serviceName" -> "X")
      status(scopedAction(requestWithW)) mustBe Status.NO_CONTENT

      val requestWithR = FakeRequest().withHeaders(s"X-Auth-Scopes-$serviceName" -> "R",
                                                   s"X-Auth-Scopes-$serviceName" -> "X")
      status(scopedAction(requestWithR)) mustBe Status.NO_CONTENT
    }

    "allow requests with wildcard scope match" in new WithScopedAction {

      val request = FakeRequest().withHeaders(s"X-Auth-Scopes-$serviceName" -> "*")
      status(scopedAction(request)) mustBe Status.NO_CONTENT
    }

    "forbid requests with wrong scopes" in new WithScopedAction {

      val request = FakeRequest().withHeaders(s"X-Auth-Scopes-$serviceName" -> "U",
                                              s"X-Auth-Scopes-$serviceName" -> "X")
      status(scopedAction(request)) mustBe Status.FORBIDDEN
    }
    "forbid requests with wildcard scope for other service" in new WithScopedAction {

      val request = FakeRequest().withHeaders(s"X-Auth-Scopes-Bauer" -> "*")
      status(scopedAction(request)) mustBe Status.FORBIDDEN
    }
  }
}
