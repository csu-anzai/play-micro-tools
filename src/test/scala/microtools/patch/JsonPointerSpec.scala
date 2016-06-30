package microtools.patch

import microtools.BusinessTry
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.JsPath
import play.api.test.Helpers._

class JsonPointerSpec extends WordSpec with MustMatchers {
  "JsonPointer" should {
    "parser empty" in {
      JsonPointer("").awaitResult mustBe BusinessTry.success(JsPath)
    }

    "parse simple path" in {
      JsonPointer("/a/b/c").awaitResult mustBe BusinessTry.success(JsPath \ "a" \ "b" \ "c")
    }

    "unescape slash and tilde" in {
      JsonPointer("/demo~10/t~0lde").awaitResult mustBe BusinessTry.success(JsPath \ "demo/0" \ "t~lde")
    }

    "Support array index" in {
      JsonPointer("/demo/12").awaitResult mustBe BusinessTry.success(JsPath \ "demo" \ 12)
    }
  }
}
