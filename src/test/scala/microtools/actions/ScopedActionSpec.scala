package microtools.actions

import microtools.models.{CustomerSubject, ExtraHeaders, ServiceName}
import microtools.{BusinessCondition, BusinessTry}
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.components.OneAppPerSuiteWithComponents
import play.api.BuiltInComponentsFromContext
import play.api.http.Status
import play.api.mvc.{AbstractController, AnyContentAsEmpty, EssentialFilter, Results}
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.reflectiveCalls

class ScopedActionSpec
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

  trait WithScopedAction {

    implicit val serviceName = ServiceName("scopedActionSpec")
    val scopeRequirement     = StandardScopeRequirements.read or StandardScopeRequirements.write
    val scopedAction = new AbstractController(components.controllerComponents) with AuthActions {
      def action = (AuthAction andThen ScopedAction(scopeRequirement)).async { implicit request =>
        val condition: BusinessCondition[String] = scopeRequirement
        condition("Test").asResult
      }
    }

    def authorizedRequest: FakeRequest[AnyContentAsEmpty.type] =
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

      val conditionalAction = new AbstractController(components.controllerComponents)
      with AuthActions {
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

  "A Customer scoped action" should {
    "only allow customer requests" in new WithScopedAction {
      val conditionalAction = new AbstractController(components.controllerComponents)
      with AuthActions {
        def action = (AuthAction andThen CustomerScopedAction(scopeRequirement)).async {
          implicit request =>
            val valueTry = BusinessTry.success("The value")
            request.subject mustBe a[CustomerSubject]

            (for {
              value <- valueTry.withCondition(StandardScopeRequirements.write)
            } yield Results.Ok(value)).asResult
        }
      }

      val nonCustomerWithW = FakeRequest()
        .withHeaders(
          ExtraHeaders.AUTH_SUBJECT_HEADER     -> "",
          ExtraHeaders.AUTH_TOKEN_HEADER       -> "",
          s"X-Auth-Scopes-${serviceName.name}" -> "W",
          s"X-Auth-Scopes-${serviceName.name}" -> "X"
        )
      val requestWithW = FakeRequest().withHeaders(
        ExtraHeaders.AUTH_SUBJECT_HEADER      -> "customer/asbc",
        ExtraHeaders.AUTH_ORGANIZATION_HEADER -> "someorg",
        ExtraHeaders.AUTH_TOKEN_HEADER        -> "",
        s"X-Auth-Scopes-${serviceName.name}"  -> "W",
        s"X-Auth-Scopes-${serviceName.name}"  -> "X"
      )
      val requestWithR = FakeRequest().withHeaders(
        ExtraHeaders.AUTH_SUBJECT_HEADER      -> "customer/asbc",
        ExtraHeaders.AUTH_ORGANIZATION_HEADER -> "someorg",
        ExtraHeaders.AUTH_TOKEN_HEADER        -> "",
        s"X-Auth-Scopes-${serviceName.name}"  -> "R",
        s"X-Auth-Scopes-${serviceName.name}"  -> "X"
      )

      status(conditionalAction.action(nonCustomerWithW)) mustBe Status.FORBIDDEN
      status(conditionalAction.action(requestWithW)) mustBe Status.OK
      status(conditionalAction.action(requestWithR)) mustBe Status.FORBIDDEN
    }
  }

  "A Service scoped action" should {
    "only allow service requests" in new WithScopedAction {
      val conditionalAction = new AbstractController(components.controllerComponents)
      with AuthActions {
        def action = (AuthAction andThen ServiceScopedAction(scopeRequirement)).async {
          implicit request =>
            val valueTry = BusinessTry.success("The value")

            (for {
              value <- valueTry.withCondition(StandardScopeRequirements.write)
            } yield Results.Ok(value)).asResult
        }
      }

      val nonCustomerWithW = FakeRequest()
        .withHeaders(
          ExtraHeaders.AUTH_SUBJECT_HEADER     -> "",
          ExtraHeaders.AUTH_TOKEN_HEADER       -> "",
          s"X-Auth-Scopes-${serviceName.name}" -> "W",
          s"X-Auth-Scopes-${serviceName.name}" -> "X"
        )
      val requestWithW = FakeRequest().withHeaders(
        ExtraHeaders.AUTH_SUBJECT_HEADER     -> "service/asbc",
        ExtraHeaders.AUTH_TOKEN_HEADER       -> "",
        s"X-Auth-Scopes-${serviceName.name}" -> "W",
        s"X-Auth-Scopes-${serviceName.name}" -> "X"
      )
      val requestWithR = FakeRequest().withHeaders(
        ExtraHeaders.AUTH_SUBJECT_HEADER     -> "service/asbc",
        ExtraHeaders.AUTH_TOKEN_HEADER       -> "",
        s"X-Auth-Scopes-${serviceName.name}" -> "R",
        s"X-Auth-Scopes-${serviceName.name}" -> "X"
      )

      status(conditionalAction.action(nonCustomerWithW)) mustBe Status.FORBIDDEN
      status(conditionalAction.action(requestWithW)) mustBe Status.OK
      status(conditionalAction.action(requestWithR)) mustBe Status.FORBIDDEN
    }
  }

  "An admin scoped action" should {
    "only allow admin requests" in new WithScopedAction {
      val conditionalAction = new AbstractController(components.controllerComponents)
      with AuthActions {
        def action = (AuthAction andThen AdminScopedAction(scopeRequirement)).async {
          implicit request =>
            val valueTry = BusinessTry.success("The value")

            (for {
              value <- valueTry.withCondition(StandardScopeRequirements.write)
            } yield Results.Ok(value)).asResult
        }
      }

      val nonCustomerWithW = FakeRequest()
        .withHeaders(
          ExtraHeaders.AUTH_SUBJECT_HEADER     -> "",
          ExtraHeaders.AUTH_TOKEN_HEADER       -> "",
          s"X-Auth-Scopes-${serviceName.name}" -> "W",
          s"X-Auth-Scopes-${serviceName.name}" -> "X"
        )
      val requestWithW = FakeRequest().withHeaders(
        ExtraHeaders.AUTH_SUBJECT_HEADER     -> "admin/asbc",
        ExtraHeaders.AUTH_TOKEN_HEADER       -> "",
        s"X-Auth-Scopes-${serviceName.name}" -> "W",
        s"X-Auth-Scopes-${serviceName.name}" -> "X"
      )
      val requestWithR = FakeRequest().withHeaders(
        ExtraHeaders.AUTH_SUBJECT_HEADER     -> "admin/asbc",
        ExtraHeaders.AUTH_TOKEN_HEADER       -> "",
        s"X-Auth-Scopes-${serviceName.name}" -> "R",
        s"X-Auth-Scopes-${serviceName.name}" -> "X"
      )

      status(conditionalAction.action(nonCustomerWithW)) mustBe Status.FORBIDDEN
      status(conditionalAction.action(requestWithW)) mustBe Status.OK
      status(conditionalAction.action(requestWithR)) mustBe Status.FORBIDDEN
    }
  }
}
