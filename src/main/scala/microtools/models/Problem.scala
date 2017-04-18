package microtools.models

import microtools.logging.{LoggingContext, WithContextAwareLogger}
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
  def asResult(implicit loggingContext: LoggingContext): Result = {
    val detailsMessage = details.map(Json.stringify).getOrElse("")
    Problem.log.info(
      s"Returning business problem as result. [ code=$code, type=${`type`}, message=$message, details=$detailsMessage ]")
    Results.Status(code)(Json.toJson(this)(Problem.jsonFormat))
  }

  def withDetails(details: String): Problem =
    withDetails(JsString(details))

  def withDetails(details: JsValue): Problem =
    this.copy(details = Some(details))
}

object Problem extends WithContextAwareLogger {
  implicit val jsonFormat = Json.format[Problem]

  def forStatus(code: Int, message: String): Problem = Problem(
    code = code,
    `type` = s"https://status.es/$code",
    message = message,
    details = None
  )
}
