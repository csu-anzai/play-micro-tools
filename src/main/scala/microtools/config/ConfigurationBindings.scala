package microtools.config

import java.time.Duration

import play.api.Configuration
import play.api.inject.{Binding, Module}

import scala.util.Try

trait ConfigurationBindings { self: Module =>

  def bindingsFor(configuration: Configuration, key: String): Seq[Binding[_]] = {
    val configs: Configuration =
      configuration.getConfig(key).getOrElse(Configuration.empty)

    configs.keys.toSeq.flatMap(bindKey(configs, _))
  }

  def bindKey(config: Configuration, key: String): Seq[Binding[_]] = {
    val bindings = Seq.newBuilder[Binding[_]]

    Try(config.underlying.getString(key)).foreach { value =>
      bindings += bind[String].qualifiedWith(key).toInstance(value)
    }
    Try(config.underlying.getBoolean(key)).foreach { value =>
      bindings += bind[Boolean].qualifiedWith(key).toInstance(value)
    }
    Try(config.underlying.getDuration(key)).foreach { value =>
      bindings += bind[Duration].qualifiedWith(key).toInstance(value)
    }
    Try(config.underlying.getLong(key)).foreach { value =>
      bindings += bind[Long].qualifiedWith(key).toInstance(value)
    }

    bindings.result()
  }
}
