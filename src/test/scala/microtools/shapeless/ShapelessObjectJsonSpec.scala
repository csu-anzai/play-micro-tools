package microtools.shapeless

import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNull, Json}
import shapeless.{:+:, ::, CNil, Coproduct, HNil, Witness}
import shapeless.syntax.singleton._
import shapeless.labelled._
import shapeless.record._

class ShapelessObjectJsonSpec extends PlaySpec with MustMatchers {
  import ShapelessObjectJson._

  "Shapeless writes" should {
    "serialize HNil to empty object" in {
      val json = Json.toJson(HNil)

      json mustBe Json.obj()
    }

    "serialize Labeled HList to object" in {
      val json =
        Json.toJson(('someInt ->> 1234) :: ('someStr ->> "string") :: ('someBool ->> true) :: HNil)

      (json \ "someInt").as[Int] mustBe 1234
      (json \ "someStr").as[String] mustBe "string"
      (json \ "someBool").as[Boolean] mustBe true
    }
  }

  "shapeless reads" should {
    "deserialize HNil on any" in {
      Json.obj("bla" -> "blub").as[HNil] mustBe HNil
      JsNull.as[HNil] mustBe HNil
    }

    "deserialize json object to labeled HList" in {
      type record = Record.`'someInt -> Int, 'someString -> String, 'someBool -> Boolean`.T
      val result = Json
        .obj("someString" -> "string", "someInt" -> 1234, "someBool" -> true)
        .as[record]

      result('someInt) mustBe 1234
      result('someString) mustBe "string"
      result('someBool) mustBe true
    }
  }
}
