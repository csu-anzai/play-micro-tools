package microtools.ws

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import microtools.BusinessSuccess
import microtools.logging.LoggingContext
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WSTrySpec
    extends WordSpec
    with MockFactory
    with MustMatchers
    with ScalaFutures {

  implicit val testContext = LoggingContext.static()
  implicit val timeout     = Timeout(1, TimeUnit.SECONDS)

  "expect Success" should {
    "return a BuisnessTry of the ok result" in {
      val okResponse = mock[WSResponse]
      (okResponse.status _).expects.returning(200).anyNumberOfTimes()

      val BusinessSuccess(response) = WSTry
        .expectSuccess(Future.successful(okResponse))
        .awaitResult

      response must be(okResponse)
    }

    "return an failing BusinessTry and log the response content (also for non json content)" in {
      val failedResponse = mock[WSResponse]
      (failedResponse.status _).expects.returning(500).anyNumberOfTimes()
      (failedResponse.json _).expects.throwing(new Exception("")).anyNumberOfTimes()
      (failedResponse.body _).expects.returning("Some error").anyNumberOfTimes()

      noException must be thrownBy WSTry.expectSuccess(
          Future.successful(failedResponse))

      val errorMessage: String = WSTry
        .expectSuccess(Future.successful(failedResponse))
        .awaitResult
        .toString
      errorMessage.toString must include("Non ok result")
    }
  }

}
