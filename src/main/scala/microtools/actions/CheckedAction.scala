package microtools.actions

import microtools.models.{Problem, Problems, RequestContext}
import play.api.http.Status
import play.api.mvc._
import play.mvc.Http.HeaderNames

import scala.concurrent.{ExecutionContext, Future}

/**
  * A controller action that performs business related checks on the requests before executing.
  */
object CheckedAction {

  case class RequestCondition(condition: RequestHeader => Boolean, problem: Problem)

  def CheckedAction[B](parser: BodyParser[B], requirements: RequestCondition*)(
      implicit ec: ExecutionContext): ActionBuilder[Request, B] =
    new ActionBuilderImpl[B](parser) {

      override def invokeBlock[A](
          request: Request[A],
          block: (Request[A]) => Future[Result]
      ): Future[Result] = {
        requirements
          .find(!_.condition(request))
          .map { failedCondition =>
            Future.successful(failedCondition.problem.asResult(RequestContext.forRequest(request)))
          }
          .getOrElse {
            block(request)
          }
      }
    }

  val RequireInternal = RequestCondition(
    rh => rh.headers.get("x-zone").contains("internal"),
    Problems.FORBIDDEN.withDetails("Only internal requests are allowed")
  )

  val RequireTLS = RequestCondition(
    rh =>
      rh.secure || rh.headers
        .get(HeaderNames.X_FORWARDED_PROTO)
        .contains("https"),
    Problem
      .forStatus(Status.UPGRADE_REQUIRED, "Upgrade required")
      .withDetails("Require secure https")
  )
}
