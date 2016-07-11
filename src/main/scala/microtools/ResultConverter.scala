package microtools

import microtools.hateoas.BusinessResult
import microtools.models.{Problem, Problems}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Results.Status
import play.api.mvc.{Result, Results}

trait ResultConverter[-R] {
  def onSuccess(result: R): Result

  def onProblem(problem: Problem): Result

  def onFailure(cause: Throwable): Result
}

object ResultConverter {
  implicit def resultResultConverter[R <: Result] = new ResultConverter[R] {
    override def onSuccess(result: R): Result = result

    override def onProblem(problem: Problem): Result = problem.asResult

    override def onFailure(cause: Throwable): Result =
      Problems.INTERNAL_SERVER_ERROR.withDetails(cause.getMessage).asResult
  }

  implicit def businessResultConverter[R <: BusinessResult] =
    new ResultConverter[R] {
      override def onSuccess(result: R): Result = result.asResult

      override def onProblem(problem: Problem): Result = problem.asResult

      override def onFailure(cause: Throwable): Result =
        Problems.INTERNAL_SERVER_ERROR.withDetails(cause.getMessage).asResult
    }

  implicit def defaultConverter[R](
      implicit writes: Writes[R]): ResultConverter[R] =
    new ResultConverter[R] {
      override def onSuccess(result: R): Result =
        Results.Ok(Json.toJson(result))

      override def onProblem(problem: Problem): Result = problem.asResult

      override def onFailure(cause: Throwable): Result =
        Problems.INTERNAL_SERVER_ERROR.withDetails(cause.getMessage).asResult
    }

  def successResultConvert[R](success: R => Result) =
    new ResultConverter[R] {
      override def onSuccess(result: R): Result =
        success(result)

      override def onProblem(problem: Problem): Result =
        problem.asResult

      override def onFailure(cause: Throwable): Result = {
        Results.InternalServerError
      }
    }

  def jsonResultConverter[R: Writes](successStatus: Status = Results.Ok) =
    successResultConvert((result: R) => successStatus(Json.toJson(result)))
}
