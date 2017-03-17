package microtools.actions

import microtools.models.Problems
import scala.concurrent.Future
import play.api.mvc.{ActionBuilder, Request, Result}

case class ServiceName(name: String) extends AnyVal

trait ScopeRequirement {
  def check(scopes: Seq[String]): Boolean
}

object ScopeRequirement {
  def pure(block: Seq[String] => Boolean): ScopeRequirement = new ScopeRequirement {
    override def check(scopes: Seq[String]): Boolean = block(scopes)
  }

  def and(leftScopeRequirement: ScopeRequirement,
          rightScopeRequirement: ScopeRequirement): ScopeRequirement = pure { scopes =>
    leftScopeRequirement.check(scopes) && rightScopeRequirement.check(scopes)
  }

  def or(leftScopeRequirement: ScopeRequirement,
         rightScopeRequirement: ScopeRequirement): ScopeRequirement = pure { scopes =>
    leftScopeRequirement.check(scopes) || rightScopeRequirement.check(scopes)
  }

  def require(scope: String): ScopeRequirement = pure { scopes =>
    scopes.contains(scope)
  }

  implicit class ScopedRequirementCombinators(scopeRequirement: ScopeRequirement) {
    def and(otherRequirement: ScopeRequirement): ScopeRequirement =
      ScopeRequirement.and(scopeRequirement, otherRequirement)
    def or(otherRequirement: ScopeRequirement): ScopeRequirement =
      ScopeRequirement.or(scopeRequirement, otherRequirement)
  }
}

case class ScopedAction(scopeRequirement: ScopeRequirement)(implicit serviceName: ServiceName)
    extends ActionBuilder[Request] {

  val authScopesHeaderName = s"X-Auth-Scopes-$serviceName"

  val wildcardScope = ScopeRequirement.require("*")

  override def invokeBlock[A](request: Request[A],
                              block: (Request[A]) => Future[Result]): Future[Result] = {
    val authScopeHeaders: Seq[String] = request.headers.getAll(authScopesHeaderName)
    val requestIsAuthorized           = (wildcardScope or scopeRequirement).check(authScopeHeaders)
    if (requestIsAuthorized) {
      block(request)
    } else {
      Future.successful(
        Problems.FORBIDDEN.withDetails("Authorization failed. Scope insufficient.").asResult)
    }
  }
}
