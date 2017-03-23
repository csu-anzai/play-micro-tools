package microtools.actions

import microtools.actions.AuthActions.AuthRequest
import microtools.models.{Problems, Scopes, ServiceName}
import play.api.mvc.ActionFunction

import scala.concurrent.Future
import play.api.mvc.Result

trait ScopeRequirement {
  def check(scopes: Scopes): Boolean
}

object ScopeRequirement {
  def pure(block: Scopes => Boolean): ScopeRequirement = new ScopeRequirement {
    override def check(scopes: Scopes): Boolean = block(scopes)
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
    extends ActionFunction[AuthRequest, AuthRequest] {

  val wildcardScope = ScopeRequirement.require("*")

  override def invokeBlock[A](request: AuthRequest[A],
                              block: (AuthRequest[A]) => Future[Result]): Future[Result] = {
    val authScopeHeaders: Scopes = request.scopes.forService(serviceName)
    val requestIsAuthorized      = (wildcardScope or scopeRequirement).check(authScopeHeaders)
    if (requestIsAuthorized) {
      block(request)
    } else {
      Future.successful(
        Problems.FORBIDDEN.withDetails("Authorization failed. Scope insufficient.").asResult)
    }
  }
}
