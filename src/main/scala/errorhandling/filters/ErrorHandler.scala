package errorhandling.filters

import javax.inject.Inject

import errorhandling.models.{Problem, Problems}
import play.api.http.HttpErrorHandler
import play.api.mvc.{RequestHeader, Result}
import play.api.{Environment, Logger}

import scala.concurrent.Future

class ErrorHandler @Inject()(implicit env: Environment)
  extends HttpErrorHandler {
  val logger = Logger("LoginController")

  override def onClientError(request: RequestHeader,
                             statusCode: Int,
                             message: String): Future[Result] = {
    implicit val loggingContext = logger.error(
      s"Client error $statusCode ${request.method} ${request.uri}: $message")

    val problem = Problem.forStatus(statusCode, "Client error").withDetails(message)

    Future.successful(problem.asResult)
  }

  override def onServerError(
                              request: RequestHeader, exception: Throwable): Future[Result] = {
    implicit val loggingContext = logger.error(
      s"Internal server error ${request.method} ${request.uri}", exception)

    val problem = Problems.INTERNAL_SERVER_ERROR.withDetails(exception.getMessage)

    Future.successful(problem.asResult)
  }
}
