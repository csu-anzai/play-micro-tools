package microtools.wire

import java.net.URL
import java.time.Duration

import com.softwaremill.tagging._

class WireTag[T](val name: String, val default: String) {
  implicit val self: this.type = this
}

/** Wire tags which can not be given by the environment */
class FixedWireTag[T](override val default: String) extends WireTag[T]("<Not given via Env>", default)

object WireTag {
  private def findEnvOrSysProp(name: String): Option[String] =
    sys.props.get(name).orElse(sys.env.get(name)).filter(_.nonEmpty)

  private def convertDuration(durationString: String): Duration = {
    val durationPattern = "(\\d+)(s|m|h)".r

    durationString match {
      case durationPattern(number: String, unit: String) =>
        unit match {
          case "s" => Duration.ofSeconds(number.toLong)
          case "m" => Duration.ofMinutes(number.toLong)
          case "h" => Duration.ofHours(number.toLong)
          case _   => throw new RuntimeException("this should not happen")
        }
      case _ =>
        throw new RuntimeException(
          s"could not convert $durationString to Duration. Expected a number followed by s (seconds), m (minutes), h (hours)"
        )
    }
  }

  def givenString[TAG <: WireTag[String]](implicit tag: TAG): String @@ TAG =
    findEnvOrSysProp(tag.name).getOrElse(tag.default).taggedWith[TAG]

  def givenBoolean[TAG <: WireTag[Boolean]](implicit tag: TAG): Boolean @@ TAG =
    findEnvOrSysProp(tag.name).getOrElse(tag.default).toBoolean.taggedWith[TAG]

  def givenInt[TAG <: WireTag[Int]](implicit tag: TAG): Int @@ TAG =
    findEnvOrSysProp(tag.name).getOrElse(tag.default).toInt.taggedWith[TAG]

  def givenLong[TAG <: WireTag[Long]](implicit tag: TAG): Long @@ TAG =
    findEnvOrSysProp(tag.name).getOrElse(tag.default).toLong.taggedWith[TAG]

  def givenDuration[TAG <: WireTag[Duration]](implicit tag: TAG): Duration @@ TAG =
    convertDuration(findEnvOrSysProp(tag.name).getOrElse(tag.default)).taggedWith[TAG]

  def givenUrl[TAG <: WireTag[URL]](implicit tag: TAG): URL @@ TAG =
    new URL(findEnvOrSysProp(tag.name).getOrElse(tag.default)).taggedWith[TAG]
}
