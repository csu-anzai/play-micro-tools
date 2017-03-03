package microtools.filters

import javax.inject.Inject

import akka.stream.Materializer
import com.codahale.metrics.MetricRegistry
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class MetricsFilter @Inject()(metricRegistry: MetricRegistry)(
    implicit ec: ExecutionContext,
    override implicit val mat: Materializer)
    extends Filter {
  private val successTimer = metricRegistry.timer("play.requests.success")
  private val clientErrorMeter =
    metricRegistry.meter("play.requests.clientError")
  private val serverErrorMeter =
    metricRegistry.meter("play.requests.serverError")

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    val timeCtx = successTimer.time()
    val futureResult  = f(rh)

    futureResult.onComplete {
      case Success(result) if result.header.status < 400 =>
        timeCtx.stop()
      case Success(result) if result.header.status < 500 =>
        clientErrorMeter.mark()
      case _ =>
        serverErrorMeter.mark()
    }
    futureResult
  }
}
