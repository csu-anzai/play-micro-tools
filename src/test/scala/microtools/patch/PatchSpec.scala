package microtools.patch

import microtools.BusinessSuccess
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsNumber, Json}
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

class PatchSpec extends WordSpec with MustMatchers {
  "Patch" should {
    "be able to replace a simple value" in {
      val source = Json.obj(
          "f1" -> "field1",
          "f2" -> 1234,
          "f3" -> Json.obj(
              "s1" -> "sub1",
              "s2" -> "sub2"
          )
      )

      val patch =
        Patch(op = PatchOperation.REPLACE, "/f2", Some(JsNumber(5678)))
      val BusinessSuccess(result) = patch(source).awaitResult

      result mustBe Json.obj(
          "f1" -> "field1",
          "f2" -> 5678,
          "f3" -> Json.obj(
              "s1" -> "sub1",
              "s2" -> "sub2"
          )
      )
    }

    "be able to add elements to an array" in {
      val source = Json.obj(
        "f1" -> Json.arr("elem1", "elem2"),
        "f2" -> 1234,
        "f3" -> Json.obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )

      val patch =
        Patch(op = PatchOperation.ADD, "/f1", Some(JsNumber(5678)))
      val BusinessSuccess(result) = patch(source).awaitResult

      result mustBe Json.obj(
        "f1" -> Json.arr("elem1", "elem2", 5678),
        "f2" -> 1234,
        "f3" -> Json.obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )
    }

    "ba able to remove a field" in {
      val source = Json.obj(
          "f1" -> "field1",
          "f2" -> 1234,
          "f3" -> Json.obj(
              "s1" -> "sub1",
              "s2" -> "sub2"
          )
      )

      val patch = Patch(op = PatchOperation.REMOVE, "/f2", None)
      val BusinessSuccess(result) = patch(source).awaitResult

      result mustBe Json.obj(
        "f1" -> "field1",
        "f3" -> Json.obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )
    }
  }
}
