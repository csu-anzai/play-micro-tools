package microtools.hateoas

import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.JsValue

import scala.concurrent.Future
import play.api.test.Helpers._

class BusinessResultSpec extends WordSpec with MustMatchers {
  import SomeActions._

  "BusinessResult" should {
    "support simple ok" in {
      val ok = BusinessResult.ok(SomeData(123, "some string"))

      val result = Future.successful(ok.asResult)
      val json   = contentAsJson(result)

      (json \ "anInt").as[Int] mustBe 123
      (json \ "aString").as[String] mustBe "some string"
      (json \ "_links").asOpt[JsValue].isEmpty mustBe true
    }

    "support ok with links" in {
      val ok =
        BusinessResult.ok(SomeData(123, "some string"), allowesActions = Seq(GetData, DeleteData))

      val result = Future.successful(ok.asResult)
      val json   = contentAsJson(result)

      (json \ "anInt").as[Int] mustBe 123
      (json \ "aString").as[String] mustBe "some string"
      (json \ "_links" \ "self" \ "href").as[String] mustBe "/data"
      (json \ "_links" \ "delete" \ "href").as[String] mustBe "/data"
    }
  }
}
