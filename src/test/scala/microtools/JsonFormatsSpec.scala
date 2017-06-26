package microtools

import microtools.models.{Subject, Token}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json._

class JsonFormatsSpec extends WordSpec with MustMatchers with GeneratorDrivenPropertyChecks {
  case class SomeDto(token: Token, subject: Subject)

  object SomeDto extends JsonFormats {
    implicit val jsonFormats = Json.format[SomeDto]
  }

  "dto" should {
    "be serializable/deserializeable" in {
      import models.Arbitraries._

      forAll { (token: Token, subject: Subject) =>
        val expected = SomeDto(token, subject)

        val JsSuccess(actual, _) = Json.fromJson[SomeDto](Json.toJson(expected))

        actual mustBe expected

      }
    }
  }

  "JsonFormat.wrapperFormat" should {
    "build a JSON Format for any wrapper case class" in {
      case class MyWrapper(count: Int)
      implicit val format = JsonFormats.wrapperFormat[MyWrapper, Int]
      JsNumber(5).as[MyWrapper] must be(MyWrapper(5))
      Json.toJson(MyWrapper(12)) must be(JsNumber(12))
    }
  }
}
