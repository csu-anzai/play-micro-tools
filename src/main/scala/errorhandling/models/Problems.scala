package errorhandling.models

import play.api.data.validation.ValidationError
import play.api.http.Status
import play.api.libs.json.{JsPath, Json}

/**
  * Collection of predefined problems most systems have to deal with.
  */
object Problems {
  val BAD_REQUEST = Problem.forStatus(Status.BAD_REQUEST, "Bad request")

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
