package microtools.config

import com.typesafe.config.ConfigValueType
import play.api.Configuration
import play.api.inject.{Binding, Module}

trait ConfigurationBindings { self: Module =>

  def bindingsFor(configuration: Configuration, key: String): Seq[Binding[_]] = {
    val configs: Configuration =
      configuration.getConfig(key).getOrElse(Configuration.empty)

    configs.entrySet.toSeq.flatMap {
      case (key, value) if value.valueType() == ConfigValueType.STRING =>
        Seq(bind[String]
          .qualifiedWith(key)
          .toInstance(value.unwrapped().asInstanceOf[String]))
      case (key, value) if value.valueType() == ConfigValueType.BOOLEAN =>
        Seq(bind[Boolean]
          .qualifiedWith(key)
          .toInstance(value.unwrapped().asInstanceOf[Boolean]))
      case _ =>
        Seq.empty
    }
  }
}
