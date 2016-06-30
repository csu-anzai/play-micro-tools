package microtools.models

import play.api.data.validation.ValidationError
import play.api.http.Status
import play.api.libs.json.{JsPath, Json}

/**
  * Collection of predefined problems most systems have to deal with.
  */
object Problems {
  val BAD_REQUEST = Problem.forStatus(Status.BAD_REQUEST, "Bad request")

  val FORBIDDEN = Problem.forStatus(Status.FORBIDDEN, "Forbidden")

  val CONFLICT = Problem.forStatus(Status.CONFLICT, "Conflict")

  val NOT_FOUND = Problem.forStatus(Status.NOT_FOUND, "Not found")

  val INTERNAL_SERVER_ERROR =
    Problem.forStatus(Status.INTERNAL_SERVER_ERROR, "Internal server error")

  def jsonValidationErrors(
      jsonErrors: Seq[(JsPath, Seq[ValidationError])]): Problem =
    BAD_REQUEST.copy(
        details = Some(Json.arr(jsonErrors.map {
      case (path, errors) =>
        Json.obj(
            "path" -> path.toString(),
            "errors" -> Json.arr(errors.map(_.messages.mkString(", ")))
        )
    })))
}
