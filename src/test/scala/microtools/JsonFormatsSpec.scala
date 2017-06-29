package microtools

import microtools.models.{Subject, Token}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Location(lat: Location.Latitude, lng: Location.Longitude)
case class MyWrapper(count: Int) extends AnyVal

object Location extends JsonFormats with AutoJsonFormats {

  case class Latitude(value: Double) extends AnyVal {
    override def toString: String = value.toString

    def +(other: Latitude): Latitude = Latitude(value + other.value)

    def -(other: Latitude): Latitude = Latitude(value - other.value)

    def *(other: Double): Latitude = Latitude(value * other)
    def /(other: Double): Latitude = Latitude(value / other)

    def meterPerLon: Double = 111320 * Math.cos(value * Math.PI / 180.0)
  }

  case class Longitude(value: Double) extends AnyVal {
    override def toString: String = value.toString

    def +(other: Longitude): Longitude = Longitude(value + other.value)

    def -(other: Longitude): Longitude = Longitude(value - other.value)

    def *(other: Double): Longitude = Longitude(value * other)
    def /(other: Double): Longitude = Longitude(value / other)
  }

  implicit val jsonWrites: OWrites[Location] = Json.writes[Location]

  implicit val jsonReads: Reads[Location] = ((__ \ "lat").read[Latitude] and
    (__ \ "lng").read[Longitude].orElse((__ \ "lon").read[Longitude]))(Location.apply _)
}

class JsonFormatsSpec extends WordSpec with MustMatchers with GeneratorDrivenPropertyChecks {
  case class SomeDto(token: Token, subject: Subject, loc: Location)

  object SomeDto extends JsonFormats with AutoJsonFormats {
    implicit val jsonFormats = Json.format[SomeDto]
  }

  "dto" should {
    "be serializable/deserializeable" in {
      import models.Arbitraries._

      forAll { (token: Token, subject: Subject) =>
        val expected =
          SomeDto(token, subject, Location(Location.Latitude(1.0), Location.Longitude(1.0)))

        val JsSuccess(actual, _) = Json.fromJson[SomeDto](Json.toJson(expected))

        actual mustBe expected

      }
    }
  }

  "JsonFormat.wrapperFormat" should {
    "build a JSON Format for any wrapper case class" in {
      implicit val format = JsonFormats.wrapperFormat[MyWrapper, Int]
      JsNumber(5).as[MyWrapper] must be(MyWrapper(5))
      Json.toJson(MyWrapper(12)) must be(JsNumber(12))
    }
  }
}
