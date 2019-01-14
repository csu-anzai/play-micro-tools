package microtools.actions

import com.codahale.metrics.MetricRegistry
import microtools.logging.WithContextAwareLogger
import microtools.models._
import play.api.mvc._
import play.mvc.Http.HeaderNames

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait TimedActions extends WithContextAwareLogger { self: AbstractController =>

  import TimedActions._

  def metricRegistry: MetricRegistry

  def TimedAction(actionId: String)(
      implicit ec: ExecutionContext): ActionBuilder[TimedRequest, AnyContent] =
    TimedAction(actionId, self.controllerComponents.parsers.default)

  def TimedAction[B](actionId: String, bodyParser: BodyParser[B])(
      implicit ec: ExecutionContext): ActionBuilder[TimedRequest, B] = {
    val timer = metricRegistry.timer(s"${log.name}.$actionId")

    new ActionBuilder[TimedRequest, B] {
      override def parser: BodyParser[B] = bodyParser

      override protected def executionContext: ExecutionContext = ec

      override def invokeBlock[A](
          request: Request[A],
          block: (TimedRequest[A]) => Future[Result]
      ): Future[Result] = {
        val businessDebug = Helper.isBusinessDebug(request)
        val flowId        = Helper.getOrCreateFlowId(request)
        val ipAddress = request.headers
          .get(HeaderNames.X_FORWARDED_FOR)
          .getOrElse(request.remoteAddress)
        val userAgent = request.headers.get(HeaderNames.USER_AGENT)
        val organization =
          Organization(request.headers.get(ExtraHeaders.AUTH_ORGANIZATION_HEADER))
        val maybeSubject =
          request.headers.get(ExtraHeaders.AUTH_SUBJECT_HEADER).map(Subject.apply)
        val forwardedHost = ForwardedHost(request.headers.get(HeaderNames.X_FORWARDED_HOST))

        implicit val meteredRequest =
          new TimedRequest(businessDebug,
                           flowId,
                           ipAddress,
                           userAgent,
                           organization,
                           maybeSubject,
                           forwardedHost,
                           request.uri,
                           request)
        val timeCtx = timer.time()
        val result =
          block(meteredRequest).map(_.withHeaders(ExtraHeaders.FLOW_ID_HEADER -> flowId))

        result.onComplete {
          case Success(_) =>
            val nanos = timeCtx.stop()
            log.info(s"$actionId: Success", "millis" -> (nanos / 1000000.0).toString)
          case Failure(e) =>
            val nanos = timeCtx.stop()
            log.error(s"$actionId: Internal error", e, "millis" -> (nanos / 1000000.0).toString)
        }
        result
      }
    }
  }
}

object TimedActions {

  class TimedRequest[A](
      override val enableBusinessDebug: Boolean,
      override val flowId: String,
      override val ipAddress: String,
      override val userAgent: Option[String],
      override val organization: Organization,
      override val maybeSubject: Option[Subject],
      override val forwardedHost: ForwardedHost,
      requestUri: String,
      request: Request[A]
  ) extends WrappedRequest[A](request)
      with RequestContext {

    override def contextValues: Seq[(String, String)] = Seq(
      "flow_id"     -> flowId,
      "request_uri" -> requestUri
    )
  }
}
