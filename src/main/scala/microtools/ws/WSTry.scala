package microtools.ws

import microtools.BusinessTry
import microtools.models.{Problem, Problems}
import play.api.http.Status
import play.api.libs.json.JsSuccess
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

object WSTry {
  def expectOkJson[T](futureResponse: Future[WSResponse]) : BusinessTry[T] = BusinessTry.future(futureResponse.map {
    case response if response.status == Status.OK =>
      BusinessTry.validateJson[T](response.json)
    case response if response.status < 400 =>
      BusinessTry.failure(Problems.INTERNAL_SERVER_ERROR.withDetails(s"Non ok result: ${response.status}"))
    case response =>
      response.json.validate[Problem] match {
        case JsSuccess(problem, _) =>
          BusinessTry.failure(problem)
        case _ =>
          BusinessTry.failure(Problems.INTERNAL_SERVER_ERROR.withDetails(s"Non ok result: ${response.status}"))
      }
  })

  def expectCreatedJson[T](futureResponse: Future[WSResponse]) : BusinessTry[T] = BusinessTry.future(futureResponse.map {
    case response if response.status == Status.CREATED =>
      BusinessTry.validateJson[T](response.json)
    case response if response.status < 400 =>
      BusinessTry.failure(Problems.INTERNAL_SERVER_ERROR.withDetails(s"Non created result: ${response.status}"))
    case response =>
      response.json.validate[Problem] match {
        case JsSuccess(problem, _) =>
          BusinessTry.failure(problem)
        case _ =>
          BusinessTry.failure(Problems.INTERNAL_SERVER_ERROR.withDetails(s"Non ok result: ${response.status}"))
      }
  })
}
