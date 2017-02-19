package microtools.shapeless

import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

sealed trait AlgebraicEnum

case class Case1Enum(id: String, val1: String) extends AlgebraicEnum

case class Case2Enum(someBool: Boolean) extends AlgebraicEnum

case class Case3Enum(someInt: Int) extends AlgebraicEnum

object AlgebraicEnum {
  object JsonDerive extends ClassNameNamingStrategy(__ \ "type") with ShapelessAlgebraicEnum {
    implicit val case1: OFormat[Case1Enum] = Json.format[Case1Enum]

    implicit val case2: OFormat[Case2Enum] = Json.format[Case2Enum]

    implicit val case3: OFormat[Case3Enum] = Json.format[Case3Enum]

    val jsonWrites: OWrites[AlgebraicEnum] = deriveWrites
    val jsonReads: Reads[AlgebraicEnum]    = deriveReads
  }

  implicit val jsonWrites: OWrites[AlgebraicEnum] = JsonDerive.jsonWrites

  implicit val jsonReads: Reads[AlgebraicEnum] = JsonDerive.jsonReads
}

class ShapelessAlgebraicEnumSpec extends PlaySpec with MustMatchers {
  "Shapeless writes" should {
    "write simple algebraic enum" in {
      val enum1: AlgebraicEnum = Case1Enum(id = "bla", val1 = "blub")
      val enum2: AlgebraicEnum = Case2Enum(someBool = true)
      val enum3: AlgebraicEnum = Case3Enum(someInt = 1234)

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

    "read simple algebraic enum" in {
      val enum1 =
        Json.obj("type" -> "Case1Enum", "id" -> "bla", "val1" -> "blub").as[AlgebraicEnum]
      val enum2 = Json.obj("type" -> "Case2Enum", "someBool" -> true).as[AlgebraicEnum]
      val enum3 = Json.obj("type" -> "Case3Enum", "someInt"  -> 1234).as[AlgebraicEnum]

      enum1 mustBe Case1Enum(id = "bla", val1 = "blub")
      enum2 mustBe Case2Enum(someBool = true)
      enum3 mustBe Case3Enum(someInt = 1234)
    }
  }
}
