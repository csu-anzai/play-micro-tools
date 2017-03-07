package microtools.config

import java.time.Duration

import play.api.Configuration
import play.api.inject.{Binding, Module}

import scala.util.Try

class BinableMapStringString(underlying: Map[String, String]) extends Map[String, String] {
  override def +[B1 >: String](kv: (String, B1)): Map[String, B1] = underlying + kv

  override def get(key: String): Option[String] = underlying.get(key)

  override def iterator: Iterator[(String, String)] = underlying.iterator

  override def -(key: String): Map[String, String] = underlying - key
}

trait ConfigurationBindings { self: Module =>

  def bindingsFor(configuration: Configuration, key: String): Seq[Binding[_]] = {
    val configs: Configuration =
      configuration.getConfig(key).getOrElse(Configuration.empty)

    configs.keys.toSeq.flatMap(bindKey(configs, _))
  }

  def bindAsMap(configuration: Configuration, name: String, key: String): Seq[Binding[_]] = {
    val configs: Configuration =
      configuration.getConfig(key).getOrElse(Configuration.empty)
    val map: Map[String, String] = configs.keys
      .map(key => key -> configs.getString(key).getOrElse(""))
      .toMap

    Seq(
      bind[BinableMapStringString]
        .qualifiedWith(name)
        .toInstance(new BinableMapStringString(map)))
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
