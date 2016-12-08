package microtools.wire


import java.net.URL

import com.softwaremill.tagging._

class WireTag[T](val name: String, val default: String) {
  implicit val self: this.type = this
}

object WireTag {
  private def findEnvOrSysProp(name: String): Option[String] =
    sys.props.get(name).orElse(sys.env.get(name)).filter(_.nonEmpty)

  def givenString[TAG <: WireTag[String]](implicit tag: TAG): String @@ TAG =
    findEnvOrSysProp(tag.name).getOrElse(tag.default).taggedWith[TAG]

  def givenBoolean[TAG <: WireTag[String]](implicit tag: TAG): Boolean @@ TAG =
    findEnvOrSysProp(tag.name).getOrElse(tag.default).toBoolean.taggedWith[TAG]

  def givenInt[TAG <: WireTag[String]](implicit tag: TAG): Int @@ TAG =
    findEnvOrSysProp(tag.name).getOrElse(tag.default).toInt.taggedWith[TAG]

  def givenUrl[TAG <: WireTag[URL]](implicit tag: TAG): URL @@ TAG =
    new URL(findEnvOrSysProp(tag.name).getOrElse(tag.default)).taggedWith[TAG]
}
