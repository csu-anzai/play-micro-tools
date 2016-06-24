package errorhandling

import akka.util.Timeout
import errorhandling.models.{Problem, Problems}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise
import scala.concurrent.duration._

class BusinessTrySpec extends WordSpec with MockFactory with MustMatchers {
  implicit val timeout = Timeout(1.second)

  "Successful BusinessTry" should {
    val aResult = "A successful result"
    val successful = BusinessTry.success(aResult)

    "a success" in {
      successful.isSuccess mustBe true
      successful.isFailure mustBe false
    }

    "be mapable" in {
      val aNewResult = "A new result"
      val mapper = mockFunction[String, String]

      mapper.expects(aResult).returns(aNewResult)

      val BusinessSuccess(result) = successful.map(mapper).awaitResult

      result mustBe aNewResult
    }

    "be flatMappable" in {
      val aNewFailure = BusinessTry.failure(Problems.INTERNAL_SERVER_ERROR)

      val mapper = mockFunction[String, BusinessTry[String]]

      mapper.expects(aResult).returns(aNewFailure)

      val BusinessFailure(problem) = successful.flatMap(mapper).awaitResult

      problem mustBe Problems.INTERNAL_SERVER_ERROR
    }

    "be filterable" in {
      val condition = mockFunction[String, Boolean]
      val aCondition = BusinessCondition(condition, Problems.BAD_REQUEST)

      condition.expects(aResult).returns(false)

      val BusinessFailure(problem) = successful.withFilter(aCondition).awaitResult

      problem mustBe Problems.BAD_REQUEST
    }

    "fold" in {
      val aNewFailure = BusinessTry.failure(Problems.INTERNAL_SERVER_ERROR)

      val onSuccess = mockFunction[String, BusinessTry[String]]
      val onFailure = mockFunction[Problem, BusinessTry[String]]

      onSuccess.expects(aResult).returns(aNewFailure)
      onFailure.expects(*).never()

      val BusinessFailure(problem) = successful.fold(onSuccess, onFailure).awaitResult

      problem mustBe Problems.INTERNAL_SERVER_ERROR
    }
  }

  "Failure BusinessTry" should {
    val failure = BusinessTry.failure(Problems.INTERNAL_SERVER_ERROR)

    "a faulure" in {
      failure.isSuccess mustBe false
      failure.isFailure mustBe true
    }

    "not be mapable" in {
      val aNewResult = "A new result"
      val mapper = mockFunction[String, String]

      mapper.expects(*).never()

      val BusinessFailure(problem) = failure.map(mapper).awaitResult

      problem mustBe Problems.INTERNAL_SERVER_ERROR
    }

    "not be flatMapable" in {
      val aNewSuccess = BusinessTry.success("A new success")

      val mapper = mockFunction[String, BusinessTry[String]]

      mapper.expects(*).never()

      val BusinessFailure(problem) = failure.flatMap(mapper).awaitResult

      problem mustBe Problems.INTERNAL_SERVER_ERROR
    }

    "not be filterable" in {
      val condition = mockFunction[String, Boolean]
      val aCondition = BusinessCondition(condition, Problems.BAD_REQUEST)

      condition.expects(*).never()

      val BusinessFailure(problem) = failure.withFilter(aCondition).awaitResult

      problem mustBe Problems.INTERNAL_SERVER_ERROR
    }

    "fold" in {
      val aNewSuccess = BusinessTry.success("A new success")

      val onSuccess = mockFunction[String, BusinessTry[String]]
      val onFailure = mockFunction[Problem, BusinessTry[String]]

      onSuccess.expects(*).never()
      onFailure.expects(Problems.INTERNAL_SERVER_ERROR).returns(aNewSuccess)

      val BusinessSuccess(result) = failure.fold(onSuccess, onFailure).awaitResult

      result mustBe "A new success"
    }
  }

  "Future/undecided BusinessTry" should {
    val aResult = "A successful result"

    "become a success if successful" in {
      val promise = Promise[String]

      val businessTry = BusinessTry.futureSuccess(promise.future)

      promise.success(aResult)

      val result = businessTry.awaitResult

      result.isSuccess mustBe true
      result.isFailure mustBe false
    }
  }
}
