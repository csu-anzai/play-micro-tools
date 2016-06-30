package microtools.models

import play.api.libs.json.{JsString, JsValue, Json}
import play.api.mvc.{Result, Results}

/**
  * Some kind of business problem that may be transferred from one service to another.
  */
case class Problem(
    code: Int,
    `type`: String,
    message: String,
    details: Option[JsValue]
) {

  /**
    * Convert the `BusinessTry` to an action result.
    */
  def asResult: Result =
    Results.Status(code)(Json.toJson(this)(Problem.jsonFormat))

  def withDetails(details: String): Problem =
    this.copy(details = Some(JsString(details)))
}

object Problem {
  implicit val jsonFormat = Json.format[Problem]

  def forStatus(code: Int, message: String): Problem = Problem(
      code = code,
      `type` = s"https://status.es/$code",
      message = message,
      details = None
  )
}
