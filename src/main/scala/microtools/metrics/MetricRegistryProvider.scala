package microtools.metrics

import javax.inject.{Provider, Singleton}

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.jvm.{
  GarbageCollectorMetricSet,
  MemoryUsageGaugeSet,
  ThreadStatesGaugeSet
}

@Singleton
class MetricRegistryProvider extends Provider[MetricRegistry] {
  private val registry = new MetricRegistry

  registry.register("jvm.gc", new GarbageCollectorMetricSet)
  registry.register("jvm.memory", new MemoryUsageGaugeSet)
  registry.register("jvm.threads", new ThreadStatesGaugeSet)

  override def get(): MetricRegistry = registry
}
