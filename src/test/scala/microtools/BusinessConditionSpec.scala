package microtools

import microtools.models.Problems
import org.scalatest.{MustMatchers, WordSpec}
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global

class BusinessConditionSpec extends WordSpec with MustMatchers {
  "BusinessCondition" should {
    "be usable as filter" in {
      val condition =
        BusinessCondition[Int](_ > 10, Problems.BAD_REQUEST.withDetails("Out of bounds"))
      val valueTry1 = BusinessTry.success(5).withCondition(condition)
      val valueTry2 = BusinessTry.success(20).withCondition(condition)

      val BusinessFailure(problem1) = valueTry1.awaitResult

      problem1 mustBe Problems.BAD_REQUEST.withDetails("Out of bounds")

      val BusinessSuccess(value2) = valueTry2.awaitResult

      value2 mustBe 20
    }

    "combine with and" in {
      val condition1 =
        BusinessCondition[Int](_ > 10, Problems.BAD_REQUEST.withDetails("Out of bounds <= 10"))
      val condition2 =
        BusinessCondition[Int](_ < 20, Problems.BAD_REQUEST.withDetails("Out of bounds >= 20"))

      val valueTry1 = BusinessTry.success(5).withCondition(condition1 and condition2)
      val valueTry2 = BusinessTry.success(30).withCondition(condition1 and condition2)
      val valueTry3 = BusinessTry.success(15).withCondition(condition1 and condition2)

      val BusinessFailure(problem1) = valueTry1.awaitResult
      val BusinessFailure(problem2) = valueTry2.awaitResult

      problem1 mustBe Problems.BAD_REQUEST.withDetails("Out of bounds <= 10")
      problem2 mustBe Problems.BAD_REQUEST.withDetails("Out of bounds >= 20")

      val BusinessSuccess(value3) = valueTry3.awaitResult

      value3 mustBe 15
    }

    "combine with or" in {
      val condition1 =
        BusinessCondition[Int](_ < 10, Problems.BAD_REQUEST.withDetails("Out of bounds >= 10"))
      val condition2 =
        BusinessCondition[Int](_ > 20, Problems.BAD_REQUEST.withDetails("Out of bounds <= 20"))

      val valueTry1 = BusinessTry.success(5).withCondition(condition1 or condition2)
      val valueTry2 = BusinessTry.success(30).withCondition(condition1 or condition2)
      val valueTry3 = BusinessTry.success(15).withCondition(condition1 or condition2)

      val BusinessSuccess(value1) = valueTry1.awaitResult
      val BusinessSuccess(value2) = valueTry2.awaitResult

      value1 mustBe 5
      value2 mustBe 30

      val BusinessFailure(problem3) = valueTry3.awaitResult

      problem3 mustBe Problems.BAD_REQUEST.withDetails("Out of bounds <= 20")
    }
  }
}
