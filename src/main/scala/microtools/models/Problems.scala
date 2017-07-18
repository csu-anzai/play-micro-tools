package microtools.models

import play.api.data.validation.ValidationError
import play.api.http.Status
import play.api.libs.json.{JsPath, Json, JsonValidationError}

/**
  * Collection of predefined problems most systems have to deal with.
  */
object Problems {
  val BAD_REQUEST: Problem = Problem.forStatus(Status.BAD_REQUEST, "Bad request")

  val UNAUTHORIZED: Problem = Problem.forStatus(Status.UNAUTHORIZED, "Unauthorized")

  val FORBIDDEN: Problem = Problem.forStatus(Status.FORBIDDEN, "Forbidden")

  val CONFLICT: Problem = Problem.forStatus(Status.CONFLICT, "Conflict")

  val NOT_FOUND: Problem = Problem.forStatus(Status.NOT_FOUND, "Not found")

  val GONE: Problem = Problem.forStatus(Status.GONE, "Gone")

  val NOT_ACCEPTABLE: Problem = Problem.forStatus(Status.NOT_ACCEPTABLE, "Not acceptable")

  val FAILED_DEPENDENCY: Problem = Problem.forStatus(Status.FAILED_DEPENDENCY, "Failed dependency")

  val INTERNAL_SERVER_ERROR: Problem =
    Problem.forStatus(Status.INTERNAL_SERVER_ERROR, "Internal server error")

  val SERVICE_UNAVAILABLE: Problem =
    Problem.forStatus(Status.SERVICE_UNAVAILABLE, "Service unavailable")

  def jsonValidationErrors(jsonErrors: Seq[(JsPath, Seq[JsonValidationError])]): Problem =
    BAD_REQUEST.copy(details = Some(Json.arr(jsonErrors.map {
      case (path, errors) =>
        Json.obj(
          "path"   -> path.toString(),
          "errors" -> Json.arr(errors.map(_.messages.mkString(", ")))
        )
    })))

  def jsonTransformErrors(jsonErrors: Seq[(JsPath, Seq[JsonValidationError])]): Problem =
    NOT_ACCEPTABLE.copy(details = Some(Json.arr(jsonErrors.map {
      case (path, errors) =>
        Json.obj(
          "path"   -> path.toString(),
          "errors" -> Json.arr(errors.map(_.messages.mkString(", ")))
        )
    })))
}
