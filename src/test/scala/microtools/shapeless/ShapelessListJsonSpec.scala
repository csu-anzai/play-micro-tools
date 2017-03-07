package microtools.shapeless

import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNull, Json}
import shapeless.HNil

class ShapelessListJsonSpec extends PlaySpec with MustMatchers {
  import ShapelessListJson._

  "Shapeless writes" should {
    "serialize HNil to null" in {
      val json = Json.toJson(HNil)

      json mustBe JsNull
    }

    "serialize generic HList to array" in {
      val json =
        Json.toJson(1234 :: "string" :: true :: HNil)

      (json \ 0).as[Int] mustBe 1234
      (json \ 1).as[String] mustBe "string"
      (json \ 2).as[Boolean] mustBe true

      Json.toJson(1234 :: "string" :: true :: HNil)
    }
  }
}
