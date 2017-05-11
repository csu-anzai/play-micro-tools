package microtools.actions

import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.json.MetricsModule
import com.fasterxml.jackson.databind.ObjectMapper
import play.api.http.HeaderNames
import play.api.mvc.{ Action, AnyContent, Controller }

trait MetricsJsonAction { self: Controller =>

  import CheckedAction._

  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, true))

  def metricRegistry: MetricRegistry

  def metrics: Action[AnyContent] = CheckedAction(RequireInternal) {
    Ok(objectMapper.writeValueAsString(metricRegistry))
      .withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")
  }
}
