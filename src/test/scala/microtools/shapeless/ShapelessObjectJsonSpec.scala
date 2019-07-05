package microtools.shapeless

import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{Json, _}
import shapeless.HNil
import shapeless.record._
import shapeless.syntax.singleton._

class ShapelessObjectJsonSpec extends PlaySpec with MustMatchers {

  "Shapeless writes" should {
    "serialize HNil to empty object" in {
      import ShapelessObjectJson._

      val json = Json.toJson(HNil)

      json mustBe Json.obj()
    }

    "serialize Labeled HList to object" in {
      import ShapelessObjectJson._

      val json =
        Json.toJson((Symbol("someInt") ->> 1234) :: (Symbol("someStr") ->> "string") :: (Symbol(
          "someBool") ->> true) :: HNil)

      (json \ "someInt").as[Int] mustBe 1234
      (json \ "someStr").as[String] mustBe "string"
      (json \ "someBool").as[Boolean] mustBe true
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

      type record =
        Record.`Symbol("someInt") -> Int, Symbol("someString") -> String, Symbol("someBool") -> Boolean`.T
      val result = Json
        .obj("someString" -> "string", "someInt" -> 1234, "someBool" -> true)
        .as[record]

      result(Symbol("someInt")) mustBe 1234
      result(Symbol("someString")) mustBe "string"
      result(Symbol("someBool")) mustBe true
    }
  }
}
