package microtools.actions

import microtools.{BusinessCondition, BusinessTry}
import microtools.logging.LoggingContext
import microtools.models._

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

trait ScopeRequirement {
  def appliesTo(scopes: Scopes): Boolean

  def checkAccess(scopes: Scopes, subject: Subject, organization: Organization)(
      implicit loggingContext: LoggingContext,
      ec: ExecutionContext
  ): BusinessTry[Boolean]
}

object ScopeRequirement {
  trait AccessCheckWithLogging {
    def check(subject: Subject, organization: Organization)(
        implicit loggingContext: LoggingContext,
        ec: ExecutionContext
    ): BusinessTry[Boolean]
  }
  type AccessCheck = PartialFunction[(Subject, Organization), BusinessTry[Boolean]]

  val wildcardScope = "*"

  val noScope = new ScopeRequirement {
    override def appliesTo(scopes: Scopes): Boolean = true

    override def checkAccess(scopes: Scopes, subject: Subject, organization: Organization)(
        implicit loggingContext: LoggingContext,
        ec: ExecutionContext): BusinessTry[Boolean] = BusinessTry.success(true)
  }

  def and(left: ScopeRequirement, right: ScopeRequirement): ScopeRequirement =
    new ScopeRequirement {
      override def appliesTo(scopes: Scopes): Boolean =
        left.appliesTo(scopes) && right.appliesTo(scopes)

      override def checkAccess(scopes: Scopes, subject: Subject, organization: Organization)(
          implicit loggingContext: LoggingContext,
          ec: ExecutionContext
      ): BusinessTry[Boolean] =
        for {
          leftAllowed  <- left.checkAccess(scopes, subject, organization)
          rightAllowed <- right.checkAccess(scopes, subject, organization)
        } yield leftAllowed && rightAllowed
    }

  def or(left: ScopeRequirement, right: ScopeRequirement): ScopeRequirement =
    new ScopeRequirement {
      override def appliesTo(scopes: Scopes): Boolean =
        left.appliesTo(scopes) || right.appliesTo(scopes)

      override def checkAccess(scopes: Scopes, subject: Subject, organization: Organization)(
          implicit loggingContext: LoggingContext,
          ec: ExecutionContext
      ): BusinessTry[Boolean] = {
        val leftApplies  = left.appliesTo(scopes)
        val rightApplies = right.appliesTo(scopes)
        for {
          leftAllowed <- if (leftApplies) left.checkAccess(scopes, subject, organization)
          else BusinessTry.success(false)
          rightAllowed <- if (rightApplies) right.checkAccess(scopes, subject, organization)
          else BusinessTry.success(false)
        } yield leftAllowed || rightAllowed
      }
    }

  def require(scope: String)(pf: AccessCheck): ScopeRequirement = {
    val accessCheck = new AccessCheckWithLogging {
      override def check(subject: Subject, organization: Organization)(
          implicit loggingContext: LoggingContext,
          ec: ExecutionContext
      ): BusinessTry[Boolean] = {
        pf.lift((subject, organization)).getOrElse(BusinessTry.success(false))
      }
    }
    require(scope, accessCheck)
  }

  def require(scope: String, accessCheck: AccessCheckWithLogging): ScopeRequirement =
    new ScopeRequirement {
      override def appliesTo(scopes: Scopes): Boolean =
        scopes.contains(wildcardScope) || scopes.contains(scope)

      override def checkAccess(scopes: Scopes, subject: Subject, organization: Organization)(
          implicit loggingContext: LoggingContext,
          ec: ExecutionContext
      ): BusinessTry[Boolean] =
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
      serviceName: ServiceName,
      ec: ExecutionContext
  ): BusinessCondition[T] = new BusinessCondition[T] {
    override def apply[R <: T](value: R): BusinessTry[R] = {
      val authScopes: Scopes = authRequestContext.scopes.forService(serviceName)

      if (!scopeRequirement.appliesTo(authScopes)) {
        BusinessTry.failure(Problems.FORBIDDEN.withDetails("Insufficient scopes"))
      } else {
        scopeRequirement
          .checkAccess(authScopes, authRequestContext.subject, authRequestContext.organization)
          .flatMap {
            case allowed if !allowed =>
              BusinessTry.failure(Problems.FORBIDDEN.withDetails("Access to resource denied"))
            case _ => BusinessTry.success(value)
          }
      }
    }
  }
}
