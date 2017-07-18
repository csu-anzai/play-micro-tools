package microtools.actions

import microtools.BusinessTry
import microtools.actions.AuthActions.{
  AdminAuthRequest,
  AuthRequest,
  CustomerAuthRequest,
  ServiceAuthRequest
}
import microtools.models._
import play.api.mvc.{ActionFunction, Result}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

sealed abstract class BaseScopedAction[R[_] <: AuthRequest[_], P[_] <: AuthRequest[_]](
    scopeRequirement: ScopeRequirement)(implicit serviceName: ServiceName, ec: ExecutionContext)
    extends ActionFunction[R, P] {
  def ensureRequestType[A](request: R[A]): BusinessTry[P[A]]

  override def invokeBlock[A](request: R[A], block: P[A] => Future[Result]): Future[Result] = {
    implicit val authContext: AuthRequestContext = request

    ensureRequestType(request)
      .withCondition(scopeRequirement)
      .flatMap { checkedRequest =>
        BusinessTry.futureSuccess(block(checkedRequest))
      }
      .asResult
  }

  override protected def executionContext: ExecutionContext = ec
}

case class ScopedAction(scopeRequirement: ScopeRequirement)(implicit serviceName: ServiceName,
                                                            ec: ExecutionContext)
    extends BaseScopedAction[AuthRequest, AuthRequest](scopeRequirement) {

  override def ensureRequestType[A](request: AuthRequest[A]): BusinessTry[AuthRequest[A]] =
    BusinessTry.success(request)
}

case class CustomerScopedAction(
    scopeRequirement: ScopeRequirement)(implicit serviceName: ServiceName, ec: ExecutionContext)
    extends BaseScopedAction[AuthRequest, CustomerAuthRequest](scopeRequirement) {
  def ensureRequestType[A](request: AuthRequest[A]): BusinessTry[CustomerAuthRequest[A]] = {
    (request.subject, request.organization) match {
      case (customer: CustomerSubject, organization: GenericOrganization) =>
        val authRequest: CustomerAuthRequest[A] =
          AuthActions.forRequest(request, customer, organization)
        BusinessTry.success(authRequest)
      case _ => BusinessTry.failure(Problems.FORBIDDEN.withDetails("Only customers are allowed"))
    }
  }
}

case class ServiceScopedAction(
    scopeRequirement: ScopeRequirement)(implicit serviceName: ServiceName, ec: ExecutionContext)
    extends BaseScopedAction[AuthRequest, ServiceAuthRequest](scopeRequirement) {
  override def ensureRequestType[A](request: AuthRequest[A]): BusinessTry[ServiceAuthRequest[A]] =
    request.subject match {
      case service: ServiceSubject =>
        val authRequest: ServiceAuthRequest[A] =
          AuthActions.forRequest(request, service, NoOrganization)
        BusinessTry.success(authRequest)
      case _ => BusinessTry.failure(Problems.FORBIDDEN.withDetails("Only services are allowed"))
    }
}

case class AdminScopedAction(scopeRequirement: ScopeRequirement)(implicit serviceName: ServiceName,
                                                                 ec: ExecutionContext)
    extends BaseScopedAction[AuthRequest, AdminAuthRequest](scopeRequirement) {
  override def ensureRequestType[A](request: AuthRequest[A]): BusinessTry[AdminAuthRequest[A]] =
    request.subject match {
      case admin: AdminSubject =>
        val authRequest: AdminAuthRequest[A] =
          AuthActions.forRequest(request, admin, NoOrganization)
        BusinessTry.success(authRequest)
      case _ => BusinessTry.failure(Problems.FORBIDDEN.withDetails("Only admins are allowed"))
    }
}
