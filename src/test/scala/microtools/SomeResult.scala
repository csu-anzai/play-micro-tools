package microtools

import play.api.libs.json.Json

case class SomeResult(anInt : Int, aString: String)

object SomeResult {
  implicit def jsonFormat = Json.format[SomeResult]
}