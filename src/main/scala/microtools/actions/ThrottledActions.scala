package microtools.actions

import java.time.Duration
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.after
import microtools.logging.WithContextAwareLogger
import microtools.models.RequestContext
import play.api.http.HeaderNames
import play.api.mvc._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

trait ThrottledActions extends WithContextAwareLogger { self: AbstractController =>
  def rateCounter: RateCounter

  def ThrottledAction(actionId: String,
                      rateWindow: Duration,
                      softLimit: Long,
                      hardLimit: Long,
                      throttle: Duration = Duration.ofMillis(100))(
      implicit ec: ExecutionContext,
      system: ActorSystem): ActionBuilder[Request, AnyContent] =
    ThrottledAction(actionId,
                    rateWindow,
                    softLimit,
                    hardLimit,
                    throttle,
                    self.controllerComponents.parsers.default)

  def ThrottledAction[B](
      actionId: String,
      rateWindow: Duration,
      softLimit: Long,
      hardLimit: Long,
      throttle: Duration,
      bodyParser: BodyParser[B]
  )(implicit ec: ExecutionContext, system: ActorSystem): ActionBuilder[Request, B] =
    new ActionBuilder[Request, B] {
      override def parser: BodyParser[B] = bodyParser

      override protected def executionContext: ExecutionContext = ec

      override def invokeBlock[A](
          request: Request[A],
          block: (Request[A]) => Future[Result]
      ): Future[Result] = {
        implicit val ctx = RequestContext.forRequest(request)
        val requestKey   = s"$actionId-${requestIP(request)}"

        rateCounter
          .incrementAndGet(requestKey, rateWindow)
          .recover {
            case e: Throwable =>
              log.error(s"Failed to get rate counter for $requestKey", e)
              0
          }
          .flatMap {
            case counter if counter > hardLimit =>
              log.warn(s"Hard rate limit reached for $requestKey")
              Future.successful(Results.TooManyRequests)
            case counter if counter > softLimit =>
              log.info(s"Soft rate limit reached for $requestKey. Throttle down.")
              after(
                FiniteDuration(
                  (counter - softLimit) * throttle.toMillis,
                  TimeUnit.MILLISECONDS
                ),
                system.scheduler
              ) {
                block(request)
              }
            case _ =>
              block(request)
          }
      }
    }

  def requestIP(request: Request[_]): String =
    request.headers
      .get(HeaderNames.X_FORWARDED_FOR)
      .flatMap(_.split(',').headOption)
      .getOrElse(request.remoteAddress)
}
