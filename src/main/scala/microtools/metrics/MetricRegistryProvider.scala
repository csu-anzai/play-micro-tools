package microtools.metrics

import java.lang.management.{ManagementFactory, MemoryPoolMXBean}

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.jvm.{
  GarbageCollectorMetricSet,
  MemoryUsageGaugeSet,
  ThreadStatesGaugeSet
}
import javax.inject.{Provider, Singleton}

import scala.collection.JavaConverters._

@Singleton
class MetricRegistryProvider extends Provider[MetricRegistry] {
  private val registry = new MetricRegistry

  registry.register("jvm.gc", new GarbageCollectorMetricSet)
  registry.register(
    "jvm.memory",
    new MemoryUsageGaugeSet(
      ManagementFactory.getMemoryMXBean,
      ManagementFactory.getMemoryPoolMXBeans.asScala.filterNot(poolNameWithNastyQuotes).asJava)
  )
  registry.register("jvm.threads", new ThreadStatesGaugeSet)

  override def get(): MetricRegistry = registry

  private def poolNameWithNastyQuotes(bean: MemoryPoolMXBean): Boolean = bean.getName.contains("'")

}
