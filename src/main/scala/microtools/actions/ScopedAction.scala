package microtools.actions

import microtools.{BusinessCondition, BusinessTry}
import microtools.actions.AuthActions.AuthRequest
import microtools.logging.LoggingContext
import microtools.models._
import play.api.mvc.{ActionFunction, Result}

import scala.concurrent.Future

trait ScopeRequirement {
  def appliesTo(scopes: Scopes): Boolean

  def checkAccess(subject: Subject, organization: Organization)(
      implicit loggingContext: LoggingContext): Boolean
}

object ScopeRequirement {
  trait AccessCheckWithLogging {
    def check(subject: Subject, organization: Organization)(
        implicit loggingContext: LoggingContext): Boolean
  }
  type AccessCheck = PartialFunction[(Subject, Organization), Boolean]

  val wildcardScope = "*"

  def and(left: ScopeRequirement, right: ScopeRequirement): ScopeRequirement =
    new ScopeRequirement {
      override def appliesTo(scopes: Scopes): Boolean =
        left.appliesTo(scopes) && right.appliesTo(scopes)

      override def checkAccess(subject: Subject, organization: Organization)(
          implicit loggingContext: LoggingContext): Boolean =
        left.checkAccess(subject, organization) && right.checkAccess(subject, organization)
    }

  def or(left: ScopeRequirement, right: ScopeRequirement): ScopeRequirement =
    new ScopeRequirement {
      override def appliesTo(scopes: Scopes): Boolean =
        left.appliesTo(scopes) || right.appliesTo(scopes)

      override def checkAccess(subject: Subject, organization: Organization)(
          implicit loggingContext: LoggingContext): Boolean =
        left.checkAccess(subject, organization) || right.checkAccess(subject, organization)
    }

  def require(scope: String)(pf: AccessCheck): ScopeRequirement = {
    val accessCheck = new AccessCheckWithLogging {
      override def check(subject: Subject, organization: Organization)(
          implicit loggingContext: LoggingContext) = {
        pf.lift((subject, organization)).getOrElse(false)
      }
    }
    require(scope, accessCheck)
  }

  def require(scope: String, accessCheck: AccessCheckWithLogging): ScopeRequirement =
    new ScopeRequirement {
      override def appliesTo(scopes: Scopes): Boolean =
        scopes.contains(wildcardScope) || scopes.contains(scope)

      override def checkAccess(subject: Subject, organization: Organization)(
          implicit loggingContext: LoggingContext): Boolean =
        accessCheck.check(subject, organization)
    }

  implicit class ScopedRequirementCombinators(scopeRequirement: ScopeRequirement) {
    def and(otherRequirement: ScopeRequirement): ScopeRequirement =
      ScopeRequirement.and(scopeRequirement, otherRequirement)
    def or(otherRequirement: ScopeRequirement): ScopeRequirement =
      ScopeRequirement.or(scopeRequirement, otherRequirement)
  }

  implicit def asBusinessCondition[T](scopeRequirement: ScopeRequirement)(
      implicit authRequestContext: AuthRequestContext,
      serviceName: ServiceName): BusinessCondition[T] = new BusinessCondition[T] {
    override def apply[R <: T](value: R): BusinessTry[R] = {
      val authScopes: Scopes = authRequestContext.scopes.forService(serviceName)

      if (!scopeRequirement.appliesTo(authScopes)) {
        BusinessTry.failure(Problems.FORBIDDEN.withDetails("Insufficient scopes"))
      } else if (!scopeRequirement.checkAccess(authRequestContext.subject,
                                               authRequestContext.organization)) {
        BusinessTry.failure(Problems.FORBIDDEN.withDetails("Access to resource denied"))
      } else {
        BusinessTry.success(value)
      }
    }
  }
}

case class ScopedAction(scopeRequirement: ScopeRequirement)(implicit serviceName: ServiceName)
    extends ActionFunction[AuthRequest, AuthRequest] {

  override def invokeBlock[A](request: AuthRequest[A],
                              block: (AuthRequest[A]) => Future[Result]): Future[Result] = {
    val authScopes: Scopes                      = request.scopes.forService(serviceName)
    implicit val loggingContext: LoggingContext = request

    if (!scopeRequirement.appliesTo(authScopes)) {
      Future.successful(Problems.FORBIDDEN.withDetails("Insufficient scopes").asResult)
    } else if (!scopeRequirement.checkAccess(request.subject, request.organization)) {
      Future.successful(Problems.FORBIDDEN.withDetails("Access to resource denied").asResult)
    } else {
      block(request)
    }
  }
}
