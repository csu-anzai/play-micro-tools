package microtools.filters

import akka.stream.Materializer
import com.codahale.metrics.MetricRegistry
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class MetricsFilter(metricRegistry: MetricRegistry)(
    implicit ec: ExecutionContext,
    override implicit val mat: Materializer)
    extends Filter {
  private val requestTimer = metricRegistry.timer("play.requests")
  private val successMeter = metricRegistry.meter("play.requests.success")
  private val clientErrorMeter =
    metricRegistry.meter("play.requests.clientError")
  private val serverErrorMeter =
    metricRegistry.meter("play.requests.serverError")

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader) = {
    val timeCtx = requestTimer.time()
    val result  = f(rh)

    result.onComplete {
      case Success(result) if result.header.status < 400 =>
        timeCtx.stop()
        successMeter.mark()
      case Success(result) if result.header.status < 500 =>
        clientErrorMeter.mark()
      case _ =>
        serverErrorMeter.mark()
    }
    result
  }
}
