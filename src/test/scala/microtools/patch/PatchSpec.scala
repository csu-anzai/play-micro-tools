package microtools.patch

import microtools.BusinessSuccess
import org.scalatest.{Assertion, MustMatchers, WordSpec}
import play.api.libs.json.{JsNumber, Json, _}

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
        Replace(__ \ "f2", JsNumber(5678))
      val BusinessSuccess(result) = patch(source)

      result mustBe Json.obj(
        "f1" -> "field1",
        "f2" -> 5678,
        "f3" -> Json.obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )
    }

    "be able to mix fields with array index" in {
      val source = Json.obj(
        "person" -> Json.obj(
          "name" -> "current"
        )
      )

      val patch = Replace(__ \ "person" \ "address",
                          Json.obj(
                            "street" -> "Somewhere"
                          ))
      val BusinessSuccess(result) = patch(source)

      result mustBe Json.obj(
        "person" -> Json.obj(
          "name" -> "current",
          "address" -> Json.obj(
            "street" -> "Somewhere"
          )
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

      val patch                   = Add(__ \ "f1", JsNumber(5678))
      val BusinessSuccess(result) = patch(source)

      result mustBe Json.obj(
        "f1" -> Json.arr("elem1", "elem2", 5678),
        "f2" -> 1234,
        "f3" -> Json.obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )
    }

    "be able to remove a field" in {
      val source = Json.obj(
        "f1" -> "field1",
        "f2" -> 1234,
        "f3" -> Json.obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )

      val patch                   = Remove(__ \ "f2")
      val BusinessSuccess(result) = patch(source)

      result mustBe Json.obj(
        "f1" -> "field1",
        "f3" -> Json.obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )
    }

    "(de-)serialize according to the RFC-6902" in {

      assertSame(Remove(__ \ "f2"), Json.obj("op" -> "remove", "path" -> "/f2"))

      assertSame(Add(__ \ "f2", JsBoolean(true)),
                 Json.obj("op" -> "add", "path" -> "/f2", "value" -> true))

      assertSame(Replace(__ \ "f2", JsBoolean(true)),
                 Json.obj("op" -> "replace", "path" -> "/f2", "value" -> true))

      def assertSame(patch: Patch, jsObject: JsObject): Assertion = {
        Json.toJson(patch) mustBe jsObject
        jsObject.as[Patch] mustBe patch
      }
    }
  }
}
