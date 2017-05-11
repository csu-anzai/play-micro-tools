package microtools

import microtools.hateoas.BusinessResult
import microtools.logging.LoggingContext
import microtools.models.{ Problem, Problems }
import play.api.libs.json.{ Json, Writes }
import play.api.mvc.Results.Status
import play.api.mvc.{ Result, Results }

trait ResultConverter[-R] {
  def onSuccess(result: R): Result

  def onProblem(problem: Problem)(implicit loggingContext: LoggingContext): Result

  def onFailure(cause: Throwable)(implicit loggingContext: LoggingContext): Result
}

object ResultConverter {
  implicit def resultResultConverter[R <: Result]: ResultConverter[R] = new ResultConverter[R] {
    override def onSuccess(result: R): Result = result

    override def onProblem(problem: Problem)(implicit loggingContext: LoggingContext): Result =
      problem.asResult

    override def onFailure(cause: Throwable)(implicit loggingContext: LoggingContext): Result =
      Problems.INTERNAL_SERVER_ERROR.withDetails(cause.getMessage).asResult
  }

  implicit def businessResultConverter[R <: BusinessResult]: ResultConverter[R] =
    new ResultConverter[R] {
      override def onSuccess(result: R): Result = result.asResult

      override def onProblem(problem: Problem)(implicit loggingContext: LoggingContext): Result =
        problem.asResult

      override def onFailure(cause: Throwable)(implicit loggingContext: LoggingContext): Result =
        Problems.INTERNAL_SERVER_ERROR.withDetails(cause.getMessage).asResult
    }

  implicit def defaultConverter[R](implicit writes: Writes[R]): ResultConverter[R] =
    new ResultConverter[R] {
      override def onSuccess(result: R): Result =
        Results.Ok(Json.toJson(result))

      override def onProblem(problem: Problem)(implicit loggingContext: LoggingContext): Result =
        problem.asResult

      override def onFailure(cause: Throwable)(implicit loggingContext: LoggingContext): Result =
        Problems.INTERNAL_SERVER_ERROR.withDetails(cause.getMessage).asResult
    }

  def successResultConverter[R](success: R => Result): ResultConverter[R] =
    new ResultConverter[R] {
      override def onSuccess(result: R): Result =
        success(result)

      override def onProblem(problem: Problem)(implicit loggingContext: LoggingContext): Result =
        problem.asResult

      override def onFailure(cause: Throwable)(implicit loggingContext: LoggingContext): Result = {
        Results.InternalServerError
      }
    }

  def jsonResultConverter[R: Writes](successStatus: Status = Results.Ok): ResultConverter[R] =
    successResultConverter((result: R) => successStatus(Json.toJson(result)))
}
