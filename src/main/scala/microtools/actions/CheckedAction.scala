package microtools.actions

import microtools.{BusinessCondition, BusinessTry}
import microtools.models.{Problem, Problems}
import play.api.http.Status
import play.api.mvc.{ActionBuilder, Request, RequestHeader, Result}
import play.mvc.Http.HeaderNames

import scala.concurrent.Future

/**
  * A controller action that performs business related checks on the requests before executing.
  */
object CheckedAction {

  case class CheckedAction(requirements: BusinessCondition[RequestHeader]*)
      extends ActionBuilder[Request] {
    override def invokeBlock[A](request: Request[A],
                                block: (Request[A]) => Future[Result]): Future[Result] = {
      requirements
        .find(!_.condition(request))
        .map { failedCondition =>
          Future.successful(failedCondition.problem.asResult)
        }
        .getOrElse {
          block(request)
        }
    }
  }

  val RequireInternal = BusinessCondition[RequestHeader](
    rh => rh.headers.get("x-zone").contains("internal"),
    Problems.FORBIDDEN.withDetails("Only internal requests are allowed"))

  val RequireTLS = BusinessCondition[RequestHeader](
    rh =>
      rh.secure || rh.headers
        .get(HeaderNames.X_FORWARDED_PROTO)
        .contains("https"),
    Problem
      .forStatus(Status.UPGRADE_REQUIRED, "Upgrade required")
      .withDetails("Require secure https")
  )
}
