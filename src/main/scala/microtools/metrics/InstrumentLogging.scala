package microtools.metrics

import javax.inject.{Inject, Singleton}

import ch.qos.logback.classic.LoggerContext
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.logback.InstrumentedAppender
import org.slf4j.{Logger, LoggerFactory}

@Singleton
class InstrumentLogging @Inject()(metricRegistry: MetricRegistry) {
  InstrumentLogging.instrument(metricRegistry)
}

object InstrumentLogging {
  def instrument(metricRegistry: MetricRegistry): Unit = {
    val factory = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    val root    = factory.getLogger(Logger.ROOT_LOGGER_NAME)

    val instrumentedAppended = new InstrumentedAppender(metricRegistry)
    instrumentedAppended.setContext(root.getLoggerContext)
    instrumentedAppended.start()
    root.addAppender(instrumentedAppended)
  }
}
