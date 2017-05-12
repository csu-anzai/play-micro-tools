package microtools.actions

import microtools.BusinessTry
import microtools.actions.AuthActions.AuthRequest
import microtools.models._
import play.api.mvc.{ActionFunction, Result}
import scala.concurrent.{ExecutionContext, Future}

case class ScopedAction(scopeRequirement: ScopeRequirement)(implicit serviceName: ServiceName,
                                                            ec: ExecutionContext)
    extends ActionFunction[AuthRequest, AuthRequest] {

  override def invokeBlock[A](
      request: AuthRequest[A],
      block: (AuthRequest[A]) => Future[Result]
  ): Future[Result] = {
    implicit val authContext: AuthRequestContext = request

    BusinessTry
      .success(request)
      .withCondition(scopeRequirement)
      .flatMap { checked_request =>
        BusinessTry.futureSuccess(block(checked_request))
      }
      .asResult
  }
}
