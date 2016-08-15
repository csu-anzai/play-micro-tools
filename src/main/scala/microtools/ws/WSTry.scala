package microtools.ws

import akka.stream.scaladsl.Source
import akka.util.ByteString
import microtools.BusinessTry
import microtools.logging.{ContextAwareLogger, LoggingContext, WithContextAwareLogger}
import microtools.models.{Problem, Problems}
import play.api.http.Status
import play.api.libs.json.{JsSuccess, Reads}
import play.api.libs.ws.{StreamedResponse, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

trait WSTry {
  def log: ContextAwareLogger

  def expectStream(futureResponse: Future[StreamedResponse])(
      implicit ec: ExecutionContext,
      ctx: LoggingContext): BusinessTry[Source[ByteString, _]] =
    BusinessTry.future(futureResponse.map {
      case response if response.headers.status < 300 =>
        BusinessTry.success(response.body)
      case response =>
        log.error(s"WS request failed with response=${response.headers}")
        BusinessTry.failure(Problems.INTERNAL_SERVER_ERROR.withDetails(
                s"Non ok result: ${response.headers}"))
    })

  def expectSuccess(futureResponse: Future[WSResponse])(
      implicit ec: ExecutionContext,
      ctx: LoggingContext): BusinessTry[WSResponse] =
    BusinessTry.future(futureResponse.map {
      case response if response.status < 300 =>
        BusinessTry.success(response)
      case response =>
        response.json.validate[Problem] match {
          case JsSuccess(problem, _) =>
            log.error(
                s"WS request failed with status=${response.status} problem=$problem")
            BusinessTry.failure(problem)
          case _ =>
            log.error(
                s"WS request failed with status=${response.status} body=${response.body}")
            BusinessTry.failure(Problems.INTERNAL_SERVER_ERROR.withDetails(
                    s"Non ok result: ${response.status}"))
        }
    })

  def expectOkJson[T](futureResponse: Future[WSResponse])(
      implicit ec: ExecutionContext,
      ctx: LoggingContext,
      reads: Reads[T]): BusinessTry[T] =
    BusinessTry.future(futureResponse.map {
      case response if response.status == Status.OK =>
        BusinessTry.validateJson[T](response.json)
      case response =>
        response.json.validate[Problem] match {
          case JsSuccess(problem, _) =>
            log.error(
                s"WS request failed with status=${response.status} problem=$problem")
            BusinessTry.failure(problem)
          case _ =>
            log.error(
                s"WS request failed with status=${response.status} body=${response.body}")
            BusinessTry.failure(Problems.INTERNAL_SERVER_ERROR.withDetails(
                    s"Non ok result: ${response.status}"))
        }
    })

  def expectCreatedJson[T](futureResponse: Future[WSResponse])(
      implicit ec: ExecutionContext,
      ctx: LoggingContext,
      reads: Reads[T]): BusinessTry[T] =
    BusinessTry.future(futureResponse.map {
      case response if response.status == Status.CREATED =>
        BusinessTry.validateJson[T](response.json)
      case response =>
        response.json.validate[Problem] match {
          case JsSuccess(problem, _) =>
            log.error(
                s"WS request failed with status=${response.status} problem=$problem")
            BusinessTry.failure(problem)
          case _ =>
            log.error(
                s"WS request failed with status=${response.status} body=${response.body}")
            BusinessTry.failure(Problems.INTERNAL_SERVER_ERROR.withDetails(
                    s"Non created result: ${response.status}"))
        }
    })
}

object WSTry extends WSTry with WithContextAwareLogger {}
