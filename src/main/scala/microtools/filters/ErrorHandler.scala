package microtools.filters

import javax.inject.Inject

import microtools.logging.WithContextAwareLogger
import microtools.models.{ExtraHeaders, Problem, Problems, RequestContext}
import play.api.http.HttpErrorHandler
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future
import scala.concurrent.Future.successful

class ErrorHandler @Inject() extends HttpErrorHandler with WithContextAwareLogger {
  override def onClientError(
      request: RequestHeader,
      statusCode: Int,
      message: String
  ): Future[Result] = {
    implicit val requestContext = RequestContext.forRequest(request)

    log.info(s"Client error $statusCode ${request.method} ${request.uri}: $message")

    val problem =
      Problem.forStatus(statusCode, "Client error").withDetails(message)

    successful(resultWithFlowId(problem.asResult))
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    implicit val requestContext = RequestContext.forRequest(request)

    log.error(s"Internal server error ${request.method} ${request.uri}", exception)

    val problem =
      Problems.INTERNAL_SERVER_ERROR

    successful(resultWithFlowId(problem.asResult))
  }

  private def resultWithFlowId(result: Result)(implicit requestContext: RequestContext): Result = {
    result.withHeaders(ExtraHeaders.FLOW_ID_HEADER -> requestContext.flowId)
  }
}
