package microtools.shapeless

import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{Json, _}
import shapeless.HNil
import shapeless.record._
import shapeless.syntax.singleton._

sealed trait AlgebraicEnum

case class Case1Enum(id: String, val1: String) extends AlgebraicEnum
object Case1Enum {
}

case class Case2Enum(someBool: Boolean) extends AlgebraicEnum
object Case2Enum {
}
case class Case3Enum(someInt: Int) extends AlgebraicEnum
object Case3Enum {
}

object AlgebraicEnum {
  object JsonDerive  extends ShapelessObjectJson {
    implicit val namingStrategy = new ClassNameNamingStrategy(__ \ "type")

    implicit val case1 :OFormat[Case1Enum] = Json.format[Case1Enum]

    implicit val case2 :OFormat[Case2Enum] = Json.format[Case2Enum]

    implicit val case3 :OFormat[Case3Enum]  = Json.format[Case3Enum]

    val jsonWrites: OWrites[AlgebraicEnum] = deriveWrites
    val jsonReads : Reads[AlgebraicEnum] = deriveReads
  }

  implicit val jsonWrites: OWrites[AlgebraicEnum] = JsonDerive.jsonWrites

  implicit val jsonReads: Reads[AlgebraicEnum] = JsonDerive.jsonReads
}

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
        Json.toJson(('someInt ->> 1234) :: ('someStr ->> "string") :: ('someBool ->> true) :: HNil)

      (json \ "someInt").as[Int] mustBe 1234
      (json \ "someStr").as[String] mustBe "string"
      (json \ "someBool").as[Boolean] mustBe true
    }

    "write simple algebraic enum" in {
      val enum1 : AlgebraicEnum = Case1Enum(id ="bla", val1="blub")
      val enum2 : AlgebraicEnum = Case2Enum(someBool = true)
      val enum3 : AlgebraicEnum = Case3Enum(someInt = 1234)

      val json1 = Json.toJson(enum1)
      val json2 = Json.toJson(enum2)
      val json3 = Json.toJson(enum3)

      (json1 \ "type").as[String] mustBe "Case1Enum"
      (json1 \ "id").as[String] mustBe "bla"
      (json1 \ "val1").as[String] mustBe "blub"
      (json2 \ "type").as[String] mustBe "Case2Enum"
      (json2 \ "someBool").as[Boolean] mustBe true
      (json3 \ "type").as[String] mustBe "Case3Enum"
      (json3 \ "someInt").as[Int] mustBe 1234
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

      type record = Record.`'someInt -> Int, 'someString -> String, 'someBool -> Boolean`.T
      val result = Json
        .obj("someString" -> "string", "someInt" -> 1234, "someBool" -> true)
        .as[record]

      result('someInt) mustBe 1234
      result('someString) mustBe "string"
      result('someBool) mustBe true
    }

    "read simple algebraic enum" in {
      val enum1 = Json.obj("type" -> "Case1Enum", "id" -> "bla", "val1" -> "blub").as[AlgebraicEnum]
      val enum2 = Json.obj("type" -> "Case2Enum", "someBool" -> true).as[AlgebraicEnum]
      val enum3 = Json.obj("type" -> "Case3Enum", "someInt" -> 1234).as[AlgebraicEnum]

      enum1 mustBe Case1Enum(id ="bla", val1="blub")
      enum2 mustBe Case2Enum(someBool = true)
      enum3 mustBe Case3Enum(someInt = 1234)
    }
  }
}
