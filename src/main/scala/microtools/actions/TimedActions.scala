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
        val businessDebug = Helper.isBusinessDebug(request)
        val flowId        = Helper.getOrCreateFlowId(request)

        implicit val meteredRequest =
          new TimedRequest(businessDebug, flowId, request.uri, request)
        val timeCtx = timer.time()
        val result  = block(meteredRequest)

        result.onComplete {
          case Success(_) =>
            val nanos = timeCtx.stop()
            log.info(s"$actionId: Success",
                     "millis" -> (nanos / 1000000.0).toString)
          case Failure(e) =>
            val nanos = timeCtx.stop()
            log.error(s"$actionId: Internal error",
                      e,
                      "millis" -> (nanos / 1000000.0).toString)
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
  ) extends WrappedRequest[A](request)
      with RequestContext {

    override def contextValues: Seq[(String, String)] = Seq(
        "flow_id"     -> flowId,
        "request_uri" -> requestUri
    )
  }
}
