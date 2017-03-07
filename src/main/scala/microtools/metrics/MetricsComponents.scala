package microtools.metrics

import com.codahale.metrics.MetricRegistry

trait MetricsComponents {
  lazy val metricRegistry: MetricRegistry = {
    val registry = new MetricRegistryProvider().get()

    InstrumentLogging.instrument(registry)

    registry
  }
}
