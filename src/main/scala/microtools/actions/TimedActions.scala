package microtools.actions

import java.util.UUID

import com.codahale.metrics.MetricRegistry
import microtools.logging.WithContextAwareLogger
import microtools.models.{ExtraHeaders, RequestContext}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait TimedActions extends WithContextAwareLogger { self: Controller =>

  import TimedActions._

  def metricRegistry: MetricRegistry

  def TimedAction(actionId: String)(implicit ec: ExecutionContext) = {
    val timer = metricRegistry.timer(s"${log.name}.$actionId")

    new ActionBuilder[TimedRequest] {
      override def invokeBlock[A](
          request: Request[A],
          block: (TimedRequest[A]) => Future[Result]): Future[Result] = {
        val businessDebug = request.cookies
          .get(ExtraHeaders.DEBUG_HEADER)
          .flatMap(c => Try(c.value.toBoolean).toOption)
          .getOrElse(request.headers
                .get(ExtraHeaders.DEBUG_HEADER)
                .flatMap(s => Try(s.toBoolean).toOption)
                .getOrElse(false))
        val flowId = request.cookies
          .get(ExtraHeaders.FLOW_ID_HEADER)
          .map(_.value)
          .getOrElse(request.headers
                .get(ExtraHeaders.FLOW_ID_HEADER)
                .getOrElse(generateFlowId()))

        implicit val meteredRequest = new TimedRequest(
            businessDebug, flowId, request.uri, request)
        val timeCtx = timer.time()
        val result  = block(meteredRequest)

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
      requestUri: String,
      request: Request[A]
  )
      extends WrappedRequest[A](request)
      with RequestContext {

    override def contextValues: Seq[(String, String)] = Seq(
        "flow_id"     -> flowId,
        "request_uri" -> requestUri
    )
  }

  def generateFlowId(): String = UUID.randomUUID().toString
}
