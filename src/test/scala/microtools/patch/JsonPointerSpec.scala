package microtools.patch

import microtools.patch.JsonPointer._
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsPath, JsString, Json}

class JsonPointerSpec extends WordSpec with MustMatchers {
  "JsonPointer" should {
    "parser empty" in {
      JsString("").as[JsPath] mustBe JsPath
    }

    "parse simple path" in {
      JsString("/a/b/c").as[JsPath] mustBe (JsPath \ "a" \ "b" \ "c")
    }

    "unescape slash and tilde" in {
      JsString("/demo~10/t~0lde").as[JsPath] mustBe (JsPath \ "demo/0" \ "t~lde")
    }

    "Support array index" in {
      JsString("/demo/12").as[JsPath] mustBe (JsPath \ "demo" \ 12)
    }

    "write empty" in {
      Json.toJson(JsPath) mustBe JsString("")
    }

    "write simple path" in {
      Json.toJson(JsPath \ "a" \ "b" \ "c") mustBe JsString("/a/b/c")
    }

    "write array index" in {
      Json.toJson(JsPath \ "demo" \ 12) mustBe JsString("/demo/12")
    }

    "write escape symbols" in {
      Json.toJson(JsPath \ "~demo~" \ "/demo") mustBe JsString("/~0demo~0/~1demo")
    }
  }
}
