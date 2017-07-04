package microtools.actions

import java.util.concurrent.{Callable, Executors, TimeUnit}

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.json.MetricsModule
import com.fasterxml.jackson.databind.ObjectMapper
import play.api.http.HeaderNames
import play.api.mvc.{Action, AnyContent, Controller}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

trait MetricsJsonAction { self: Controller =>

  import CheckedAction._

  private val pool = Executors.newSingleThreadExecutor()
  val objectMapper = new ObjectMapper()
  implicit def executionContext: ExecutionContext
  def metricRegistry: MetricRegistry

  objectMapper.registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, true))

  private def computeMetricsBlocking(): String = {
    objectMapper.writeValueAsString(metricRegistry)
  }

  def computeMetrics(): Future[String] = {
    val metrics: Promise[String] = Promise()
    pool.submit(new MetricRunner(metrics))
    metrics.future
  }

  def metrics: Action[AnyContent] = CheckedAction(RequireInternal).async {
    computeMetrics()
      .map(Ok.apply(_))
      .map(_.withHeaders(HeaderNames.CONTENT_TYPE -> "application/json"))
  }

  class MetricRunner(promise: Promise[String]) extends Runnable {
    override def run(): Unit = {
      try {
        val metrics = computeMetricsBlocking()
        promise.success(metrics)
      } catch {
        case NonFatal(e) => promise.failure(e)
      }
    }
  }
}
