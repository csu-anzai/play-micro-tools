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
}
