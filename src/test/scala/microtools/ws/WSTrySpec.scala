package microtools.ws

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import microtools.BusinessSuccess
import microtools.logging.LoggingContext
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WSTrySpec extends WordSpec with MockFactory with MustMatchers with ScalaFutures {

  implicit val testContext = LoggingContext.static()
  implicit val timeout     = Timeout(1, TimeUnit.SECONDS)

  "expect Success" should {
    "return a BusinessTry of the ok result" in {
      val okResponse = mock[WSResponse]
      (okResponse.status _).expects.returning(200).anyNumberOfTimes()

      val BusinessSuccess(response) = WSTry
        .expectSuccess(Future.successful(okResponse))
        .awaitResult

      response must be(okResponse)
    }

    "return a failing BusinessTry and log the response content (also for non json content)" in {
      val failedResponse = new WSResponse {
        def allHeaders: Map[String, scala.collection.Seq[String]]                       = ???
        override def body: String                                                       = "Some error"
        override def bodyAsBytes: akka.util.ByteString                                  = ???
        override def bodyAsSource: akka.stream.scaladsl.Source[akka.util.ByteString, _] = ???
        override def cookie(name: String): Option[play.api.libs.ws.WSCookie]            = ???
        override def cookies: scala.collection.Seq[play.api.libs.ws.WSCookie]           = ???
        override def headers: Map[String, scala.collection.Seq[String]]                 = ???
        def json: play.api.libs.json.JsValue                                            = throw new Exception("")
        override def status: Int                                                        = 500
        override def statusText: String                                                 = ???
        override def underlying[T]: T                                                   = ???
        def xml: scala.xml.Elem                                                         = ???
        def uri: java.net.URI                                                           = ???

      }
      noException must be thrownBy WSTry.expectSuccess(Future.successful(failedResponse))

      val errorMessage: String = WSTry
        .expectSuccess(Future.successful(failedResponse))
        .awaitResult
        .toString
      errorMessage.toString must include("Non ok result")
    }

    "return a failing BusinessTry and log the response content (also for non json content) when server lies" in {
      val failedResponse = new WSResponse {
        def allHeaders: Map[String, scala.collection.Seq[String]]                       = ???
        override def body: String                                                       = "{{{raw body that's not json because the server lies"
        override def bodyAsBytes: akka.util.ByteString                                  = ???
        override def bodyAsSource: akka.stream.scaladsl.Source[akka.util.ByteString, _] = ???
        override def cookie(name: String): Option[play.api.libs.ws.WSCookie]            = ???
        override def cookies: scala.collection.Seq[play.api.libs.ws.WSCookie]           = ???
        override def headers: Map[String, scala.collection.Seq[String]]                 = ???
        def json: play.api.libs.json.JsValue                                            = throw new Exception("parse error")
        override def status: Int                                                        = 200
        override def statusText: String                                                 = ???
        override def underlying[T]: T                                                   = ???
        def xml: scala.xml.Elem                                                         = ???
        def uri: java.net.URI                                                           = ???

      }

      noException must be thrownBy WSTry.expectOkJson[JsValue](Future.successful(failedResponse))

      val errorMessage: String = WSTry
        .expectOkJson[JsValue](Future.successful(failedResponse))
        .awaitResult
        .toString
      errorMessage.toString must include(
        "JSON parsing (not validation) from remote ok response failed")
    }
  }

}
