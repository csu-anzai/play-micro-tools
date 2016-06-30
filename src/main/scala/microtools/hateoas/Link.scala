package microtools.hateoas

import play.api.libs.json.Json

case class Link(
               href : String,
               method: Option[String] = None,
               templated: Option[Boolean] = None
               )

object Link {
  implicit val jsonFormat = Json.format[Link]
}