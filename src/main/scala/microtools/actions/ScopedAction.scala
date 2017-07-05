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
      .flatMap { checked_request =>
        BusinessTry.futureSuccess(block(checked_request))
      }
      .asResult
  }
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
        BusinessTry.success(
          new CustomerAuthRequest(
            enableBusinessDebug = request.enableBusinessDebug,
            flowId = request.flowId,
            subject = customer,
            organization = organization,
            scopes = request.scopes,
            token = request.token,
            ipAddress = request.ipAddress,
            userAgent = request.userAgent,
            requestUri = request.uri,
            request = request
          ))
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
        BusinessTry.success(
          new ServiceAuthRequest(
            enableBusinessDebug = request.enableBusinessDebug,
            flowId = request.flowId,
            subject = service,
            organization = NoOrganization,
            scopes = request.scopes,
            token = request.token,
            ipAddress = request.ipAddress,
            userAgent = request.userAgent,
            requestUri = request.uri,
            request = request
          ))
      case _ => BusinessTry.failure(Problems.FORBIDDEN.withDetails("Only services are allowed"))
    }
}

case class AdminScopedAction(scopeRequirement: ScopeRequirement)(implicit serviceName: ServiceName,
                                                                 ec: ExecutionContext)
    extends BaseScopedAction[AuthRequest, AdminAuthRequest](scopeRequirement) {
  override def ensureRequestType[A](request: AuthRequest[A]): BusinessTry[AdminAuthRequest[A]] =
    request.subject match {
      case admin: AdminSubject =>
        BusinessTry.success(
          new AdminAuthRequest(
            enableBusinessDebug = request.enableBusinessDebug,
            flowId = request.flowId,
            subject = admin,
            organization = NoOrganization,
            scopes = request.scopes,
            token = request.token,
            ipAddress = request.ipAddress,
            userAgent = request.userAgent,
            requestUri = request.uri,
            request = request
          ))
      case _ => BusinessTry.failure(Problems.FORBIDDEN.withDetails("Only admins are allowed"))
    }
}
