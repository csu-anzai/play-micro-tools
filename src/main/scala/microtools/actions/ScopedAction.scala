package microtools.actions

import microtools.actions.AuthActions.AuthRequest
import microtools.models._
import play.api.mvc.ActionFunction

import scala.concurrent.Future
import play.api.mvc.Result

trait ScopeRequirement {
  def appliesTo(scopes: Scopes): Boolean

  def checkAccess(subject: Subject, organization: Organization): Boolean
}

object ScopeRequirement {
  type AccessCheck = PartialFunction[(Subject, Organization), Boolean]

  val wildcardScope = "*"

  def and(left: ScopeRequirement, right: ScopeRequirement): ScopeRequirement =
    new ScopeRequirement {
      override def appliesTo(scopes: Scopes): Boolean =
        left.appliesTo(scopes) && right.appliesTo(scopes)

      override def checkAccess(subject: Subject, organization: Organization): Boolean =
        left.checkAccess(subject, organization) && right.checkAccess(subject, organization)
    }

  def or(left: ScopeRequirement, right: ScopeRequirement): ScopeRequirement =
    new ScopeRequirement {
      override def appliesTo(scopes: Scopes): Boolean =
        left.appliesTo(scopes) || right.appliesTo(scopes)

      override def checkAccess(subject: Subject, organization: Organization): Boolean =
        left.checkAccess(subject, organization) || right.checkAccess(subject, organization)
    }

  def require(scope: String)(block: AccessCheck): ScopeRequirement =
    new ScopeRequirement {
      override def appliesTo(scopes: Scopes): Boolean =
        scopes.contains(wildcardScope) || scopes.contains(scope)

      override def checkAccess(subject: Subject, organization: Organization): Boolean =
        block.applyOrElse((subject, organization), (_: (Subject, Organization)) => false)
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

  override def invokeBlock[A](request: AuthRequest[A],
                              block: (AuthRequest[A]) => Future[Result]): Future[Result] = {
    val authScopes: Scopes = request.scopes.forService(serviceName)

    if (!scopeRequirement.appliesTo(authScopes)) {
      Future.successful(Problems.FORBIDDEN.withDetails("Insufficient scopes").asResult)
    } else if (!scopeRequirement.checkAccess(request.subject, request.organization)) {
      Future.successful(Problems.FORBIDDEN.withDetails("Access to resource denied").asResult)
    } else {
      block(request)
    }
  }
}
