package microtools.hateoas

import play.api.libs.json.Json

case class SomeData(anInt: Int, aString: String)

object SomeData {
  implicit val jsonFormat = Json.format[SomeData]
}
