package microtools.shapeless

import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNull, Json}
import shapeless.{::, HNil, Witness}
import shapeless.syntax.singleton._
import shapeless.labelled._
import shapeless.record._

class ShapelessJsonSpec extends PlaySpec with MustMatchers {

  "Shapeless writes" should {
    "serialize HNil to jsNull" in {
      import ShapelessObjectJson._

      val json = Json.toJson(HNil)

      json mustBe JsNull
    }

    "serialize Labeled HList to object" in {
      import ShapelessObjectJson._

      val json =
        Json.toJson(('someInt ->> 1234) :: ('someStr ->> "string") :: ('someBool ->> true) :: HNil)

      (json \ "someInt").as[Int] mustBe 1234
      (json \ "someStr").as[String] mustBe "string"
      (json \ "someBool").as[Boolean] mustBe true
    }

    "serialize generic HList to array" in {
      import ShapelessListJson._

      val json =
        Json.toJson(1234 :: "string" :: true :: HNil)

      (json \ 0).as[Int] mustBe 1234
      (json \ 1).as[String] mustBe "string"
      (json \ 2).as[Boolean] mustBe true

      Json.toJson(1234 :: "string" :: true :: HNil)
    }
  }

  "shapeless reads" should {
    "deserialize HNil on any" in {
      import ShapelessObjectJson._

      Json.obj("bla" -> "blub").as[HNil] mustBe HNil
      JsNull.as[HNil] mustBe HNil
    }

    "deserialize json object to labeled HList" in {
      import ShapelessObjectJson._

      val someInt    = Witness('someInt)
      val someString = Witness('someString)
      val someBool = Witness('someBool)
      val result = Json
        .obj("someString" -> "string", "someInt" -> 1234, "someBool" -> true)
        .as[FieldType[someInt.T, Int] :: FieldType[someBool.T, Boolean] :: FieldType[someString.T, String] :: HNil]

      result('someInt) mustBe 1234
      result('someString) mustBe "string"
      result('someBool) mustBe true
    }
  }
}
