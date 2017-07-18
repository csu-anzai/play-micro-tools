package microtools.ws

import akka.stream.scaladsl.Source
import akka.util.ByteString
import microtools.logging.{ContextAwareLogger, LoggingContext, WithContextAwareLogger}
import microtools.models.{Problem, Problems}
import microtools.{BusinessFailure, BusinessTry}
import play.api.http.Status
import play.api.libs.json.Reads
import play.api.libs.ws.ahc.StreamedResponse
import play.api.libs.ws.WSResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Success, Try}

trait WSTry {
  def log: ContextAwareLogger

  def expectStream(futureResponse: Future[StreamedResponse])(
      implicit ec: ExecutionContext,
      ctx: LoggingContext
  ): BusinessTry[Source[ByteString, _]] =
    BusinessTry.future(futureResponse.map {
      case response if response.status < 300 =>
        BusinessTry.success(response.bodyAsSource)
      case response =>
        log.error(s"WS request failed with response=${response.headers}")
        BusinessTry.failure(
          Problems.INTERNAL_SERVER_ERROR.withDetails(s"Non ok result: ${response.headers}")
        )
    })

  def expectSuccess(futureResponse: Future[WSResponse])(
      implicit ec: ExecutionContext,
      ctx: LoggingContext
  ): BusinessTry[WSResponse] =
    BusinessTry.future(futureResponse.map {
      case response if response.status < 300 =>
        BusinessTry.success(response)
      case response =>
        notTheExpectedResponse("ok", response)
    })

  def expectOkJson[T](futureResponse: Future[WSResponse])(implicit ec: ExecutionContext,
                                                          ctx: LoggingContext,
                                                          reads: Reads[T]): BusinessTry[T] =
    BusinessTry.future(futureResponse.map {
      case response if response.status == Status.OK =>
        try {
          BusinessTry.validateJson[T](response.json)
        } catch {
          case NonFatal(e) =>
            log.error(
              s"Json parsing (not validation) blew up although the HTTP status code from the response was ok! For reference, the raw json body is '${response.body}'",
              e
            )
            BusinessTry.failure(
              Problems.FAILED_DEPENDENCY.withDetails(
                "JSON parsing (not validation) from remote ok response failed"))
        }
      case response =>
        notTheExpectedResponse("ok", response)
    })

  def expectCreatedJson[T](futureResponse: Future[WSResponse])(implicit ec: ExecutionContext,
                                                               ctx: LoggingContext,
                                                               reads: Reads[T]): BusinessTry[T] =
    BusinessTry.future(futureResponse.map {
      case response if response.status == Status.CREATED =>
        BusinessTry.validateJson[T](response.json)
      case response =>
        notTheExpectedResponse("created", response)
    })

  private def notTheExpectedResponse(expected: String, response: WSResponse)(
      implicit ec: LoggingContext
  ): BusinessFailure[Nothing] = {
    Try(response.json.as[Problem]) match {
      case Success(problem) =>
        log.error(s"WS request failed with status=${response.status} problem=$problem")

        BusinessFailure(problem)
      case _ =>
        log.error(s"WS request failed with status=${response.status} body=${response.body}")

        BusinessFailure(
          Problems.INTERNAL_SERVER_ERROR.withDetails(s"Non $expected result: ${response.status}")
        )
    }
  }
}

object WSTry extends WSTry with WithContextAwareLogger
