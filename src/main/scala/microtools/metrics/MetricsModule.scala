package microtools.metrics

import com.codahale.metrics.MetricRegistry
import play.api.inject.{ Binding, Module }
import play.api.{ Configuration, Environment }

class MetricsModule(environment: Environment, configuration: Configuration) extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[MetricRegistry].toProvider[MetricRegistryProvider],
      bind[InstrumentLogging].toSelf.eagerly()
    )
  }
}
