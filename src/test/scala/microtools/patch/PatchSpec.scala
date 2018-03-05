package microtools.patch

import microtools.{BusinessFailure, BusinessSuccess}
import org.scalatest.{Assertion, MustMatchers, WordSpec}
import play.api.libs.json.{JsNumber, Json, _}
import play.api.http.Status._
import Patch.applyPatches
import play.api.libs.json.Json.obj

class PatchSpec extends WordSpec with MustMatchers {
  val whitelist = PatchWhitelist(
    Seq(__ \ "f2", __ \ "f1", __ \ "person" \ "address", __ \ "f3", __ \ "f3" \ "s1", __ \ "f4"))
  "Patch" should {
    "be able to replace a simple value" in {
      val source = obj(
        "f1" -> "field1",
        "f2" -> 1234,
        "f3" -> obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )

      val patch =
        Replace(__ \ "f2", JsNumber(5678))
      val BusinessSuccess(result) = patch(source, whitelist)

      result mustBe obj(
        "f1" -> "field1",
        "f2" -> 5678,
        "f3" -> obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )
    }

    "be able to replace a nested value when the upper path is whitelisted" in {

      val source = obj(
        "f1" -> "field1",
        "f2" -> 1234,
        "f3" -> obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )

      // s2 is not on the whitelist but f3 is
      val patch =
        Replace(__ \ "f3" \ "s2", JsString("spxsnezre"))
      val BusinessSuccess(result) = patch(source, whitelist)

      result mustBe obj(
        "f1" -> "field1",
        "f2" -> 1234,
        "f3" -> obj(
          "s1" -> "sub1",
          "s2" -> "spxsnezre"
        )
      )
    }

    "be not be able to replace a nested value when the upper path is whitelisted" in {

      val whitelist = PatchWhitelist(Seq(__ \ "foo" \ "bar"))

      val source = obj(
        "foo" -> obj("bar" -> true, "no" -> "un touchable")
      )

      // s2 is not on the whitelist but f3 is
      val patch =
        Replace(__ \ "for" \ "no", JsString(""))

      patch(source, whitelist) mustBe a[BusinessFailure]
    }

    "be able to replace a nested value" in {
      val source = obj(
        "f1" -> "field1",
        "f2" -> 1234,
        "f3" -> obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )

      val patch =
        Replace(__ \ "f3" \ "s1", JsString("sub_new"))
      val BusinessSuccess(result) = patch(source, whitelist)

      result mustBe obj(
        "f1" -> "field1",
        "f2" -> 1234,
        "f3" -> obj(
          "s1" -> "sub_new",
          "s2" -> "sub2"
        )
      )
    }

    "be able to replace a nested object" in {
      val source = obj(
        "f1" -> "field1",
        "f2" -> 1234,
        "f3" -> obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )

      val patch =
        Replace(__ \ "f3", obj("s3" -> JsString("sub3")))
      val BusinessSuccess(result) = patch(source, whitelist)

      result mustBe obj(
        "f1" -> "field1",
        "f2" -> 1234,
        "f3" -> obj(
          "s3" -> "sub3"
        )
      )
    }

    "be able to add a new value" in {
      val source = obj(
        "f1" -> "field1",
        "f3" -> JsNull
      )

      val patch =
        Add(__ \ "f3", JsString("meep"))
      val BusinessSuccess(result) = patch(source, whitelist)

      result mustBe obj(
        "f1" -> "field1",
        "f3" -> "meep"
      )

    }

    "be able to add a new key/value pair" in {
      val source = obj(
        "f1" -> "field1"
      )

      val patch =
        Add(__ \ "f3", JsString("meep"))
      val BusinessSuccess(result) = patch(source, whitelist)

      result mustBe obj(
        "f1" -> "field1",
        "f3" -> "meep"
      )
    }

    "not be able to operate if json path is not on whitelist" in {
      val source = obj(
        "f1"               -> "field1",
        "f2"               -> 1234,
        "not-on-whitelist" -> "gegenbauer",
        "f3" -> obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )

      val patch =
        Replace(__ \ "not-on-whitelist", JsString("bowerfeind"))
      val BusinessFailure(problem) = patch(source, whitelist)

      problem.code mustEqual FORBIDDEN
    }

    "be able to mix fields with array index" in {
      val source = obj(
        "person" -> obj(
          "name" -> "current"
        )
      )

      val patch = Replace(__ \ "person" \ "address",
                          obj(
                            "street" -> "Somewhere"
                          ))
      val BusinessSuccess(result) = patch(source, whitelist)

      result mustBe obj(
        "person" -> obj(
          "name" -> "current",
          "address" -> obj(
            "street" -> "Somewhere"
          )
        )
      )
    }

    "be able to add elements to an array" in {
      val source = obj(
        "f1" -> Json.arr("elem1", "elem2"),
        "f2" -> 1234,
        "f3" -> obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )

      val patch                   = Add(__ \ "f1", JsNumber(5678))
      val BusinessSuccess(result) = patch(source, whitelist)

      result mustBe obj(
        "f1" -> Json.arr("elem1", "elem2", 5678),
        "f2" -> 1234,
        "f3" -> obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )
    }

    "be able to remove a non-existing field" in {
      val source = obj(
        "f1" -> "field1",
        "f3" -> obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )

      val patch                   = Remove(__ \ "f2")
      val BusinessSuccess(result) = patch(source, whitelist)

      result mustBe source
    }

    "be able to remove a field" in {
      val source = obj(
        "f1" -> "field1",
        "f2" -> 1234,
        "f3" -> obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )

      val patch                   = Remove(__ \ "f2")
      val BusinessSuccess(result) = patch(source, whitelist)

      result mustBe obj(
        "f1" -> "field1",
        "f3" -> obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )
    }

    "(de-)serialize according to the RFC-6902" in {

      assertSame(Remove(__ \ "f2"), obj("op" -> "remove", "path" -> "/f2"))

      assertSame(Add(__ \ "f2", JsBoolean(true)),
                 obj("op" -> "add", "path" -> "/f2", "value" -> true))

      assertSame(Replace(__ \ "f2", JsBoolean(true)),
                 obj("op" -> "replace", "path" -> "/f2", "value" -> true))

      def assertSame(patch: Patch, jsObject: JsObject): Assertion = {
        Json.toJson(patch) mustBe jsObject
        jsObject.as[Patch] mustBe patch
      }
    }
  }

  "Apply Patches" should {
    "not change object" in {
      val source = obj(
        "f1" -> "field1",
        "f2" -> 1234
      )

      val BusinessSuccess(patched) = applyPatches(List.empty, whitelist)(source)

      patched mustBe source
    }

    "apply a single patch" in {
      val source = obj(
        "f1" -> "field1",
        "f2" -> 1234
      )

      val BusinessSuccess(patched) = applyPatches(List(Remove(__ \ "f1")), whitelist)(source)

      patched mustBe obj(
        "f2" -> 1234
      )
    }

    "apply multiple patches" in {
      val source = obj(
        "f1" -> "field1",
        "f2" -> 1234
      )

      val BusinessSuccess(patched) =
        applyPatches(List(Remove(__ \ "f1"), Replace(__ \ "f2", JsString(""))), whitelist)(source)

      patched mustBe (source - "f1" + ("f2" -> JsString("")))
    }

    "propagate errors" in {

      val source = obj(
        "f1" -> "field1"
      )

      val failure = applyPatches(List(Replace(__ \ "unknown", JsString(""))), whitelist)(source)

      failure mustBe a[BusinessFailure]
    }

    "add nested field with replace" in {

      val source = obj(
        "f1" -> "field1",
        "f2" -> 1234,
        "f3" -> obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )

      // s2 is not on the whitelist but f3 is
      val patch =
        Replace(__ \ "f4" \ "s2", JsString("spxsnezre"))
      val BusinessSuccess(result) = patch(source, whitelist)

      result mustBe obj(
        "f1" -> "field1",
        "f2" -> 1234,
        "f3" -> obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        ),
        "f4" -> obj(
          "s2" -> "spxsnezre"
        )
      )
    }

    "remove non existing nested field" in {

      val source = obj(
        "f1" -> "field1",
        "f2" -> 1234,
        "f3" -> obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )

      // s2 is not on the whitelist but f3 is
      val patch =
        Remove(__ \ "f4" \ "f5")
      val BusinessSuccess(result) = patch(source, whitelist)

      result mustBe obj(
        "f1" -> "field1",
        "f2" -> 1234,
        "f3" -> obj(
          "s1" -> "sub1",
          "s2" -> "sub2"
        )
      )
    }

  }
}
