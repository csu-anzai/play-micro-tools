package microtools.shapeless

import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsBoolean, JsNumber, JsString, Json}
import shapeless._

class ShapelessCoproductJsonSpec extends PlaySpec with MustMatchers {
  import ShapelessCoproductJson._

  "Shapeless writes" should {
    "serialize a simple coproduct" in {
      type ISB = Int :+: String :+: Boolean :+: CNil

      val isb1 = Coproduct[ISB](1234)
      val isb2 = Coproduct[ISB]("string")
      val isb3 = Coproduct[ISB](true)

      Json.toJson(isb1) mustBe JsNumber(1234)
      Json.toJson(isb2) mustBe JsString("string")
      Json.toJson(isb3) mustBe JsBoolean(true)
    }
  }

  "Shapeless reads" should {
    "deserialize a simple coproduct" in {
      type ISB = Int :+: String :+: Boolean :+: CNil

      val isb1 = JsBoolean(true).as[ISB]
      val isb2 = JsString("string").as[ISB]
      val isb3 = JsNumber(1234).as[ISB]

      isb1.select[Boolean] mustBe Some(true)
      isb2.select[String] mustBe Some("string")
      isb3.select[Int] mustBe Some(1234)
    }
  }
}
