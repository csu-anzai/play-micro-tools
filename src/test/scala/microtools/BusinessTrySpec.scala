package microtools

import microtools.logging.LoggingContext
import microtools.models.{Problem, Problems}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}
import play.mvc.Http.Status
import play.api.test.Helpers._
import scala.util.Try
import scala.util.Failure

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

class BusinessTrySpec extends WordSpec with MockFactory with MustMatchers with ScalaFutures {

  private implicit val loggingContext = LoggingContext.static()

  "Successful BusinessTry" should {
    val aResult    = SomeResult(1234, "A successful result")
    val successful = BusinessTry.success(aResult)

    "a success" in {
      successful.awaitResult.isSuccess mustBe true
      successful.awaitResult.isFailure mustBe false
    }

    "be mapable" in {
      val aNewResult = SomeResult(5432, "A new result")
      val mapper     = mockFunction[SomeResult, SomeResult]

      mapper.expects(aResult).returns(aNewResult)

      val BusinessSuccess(result) = successful.map(mapper).awaitResult

      result mustBe aNewResult
    }

    "be flatMappable" in {
      val aNewFailure = BusinessTry.failure(Problems.INTERNAL_SERVER_ERROR)

      val mapper = mockFunction[SomeResult, BusinessTry[SomeResult]]

      mapper.expects(aResult).returns(aNewFailure)

      val BusinessFailure(problem) = successful.flatMap(mapper).awaitResult

      problem mustBe Problems.INTERNAL_SERVER_ERROR
    }

    "be filterable" in {
      val condition  = mockFunction[SomeResult, Boolean]
      val aCondition = BusinessCondition(condition, Problems.BAD_REQUEST)

      condition.expects(aResult).returns(false)

      val BusinessFailure(problem) =
        successful.withCondition(aCondition).awaitResult

      problem mustBe Problems.BAD_REQUEST
    }

    "be lifted from success try" in {
      val successTry               = Try("gegenbauer")
      val BusinessSuccess(success) = BusinessTry.fromTry(successTry)(_ => Problems.BAD_REQUEST)
      success mustEqual "gegenbauer"
    }

    "be lifted from failure try" in {
      val failureTry               = Failure(new RuntimeException("Who you gonna call? Bauerbusters!"))
      val BusinessFailure(problem) = BusinessTry.fromTry(failureTry)(_ => Problems.BAD_REQUEST)
      problem mustEqual Problems.BAD_REQUEST
    }

    "fold" in {
      val aNewFailure =
        BusinessTry.failure(Problems.INTERNAL_SERVER_ERROR)

      val onSuccess = mockFunction[SomeResult, BusinessTry[SomeResult]]
      val onFailure = mockFunction[Problem, BusinessTry[SomeResult]]

      onSuccess.expects(aResult).returns(aNewFailure)
      onFailure.expects(*).never()

      val BusinessFailure(problem) =
        successful.fold(onSuccess, onFailure).awaitResult

      problem mustBe Problems.INTERNAL_SERVER_ERROR
    }

    "convert to ok with json by default" in {
      val result = successful.asResult

      status(result) mustBe Status.OK
      contentAsJson(result) mustBe Json.obj(
        "anInt"   -> aResult.anInt,
        "aString" -> aResult.aString
      )
    }

    "can be converted to any other result" in {
      implicit val converter = new ResultConverter[SomeResult] {
        override def onSuccess(result: SomeResult): Result =
          Results
            .Accepted(result.aString)
            .withHeaders("X-Int" -> result.anInt.toString)

        override def onProblem(problem: Problem)(implicit loggingContext: LoggingContext): Result =
          ???

        override def onFailure(cause: Throwable)(implicit loggingContext: LoggingContext): Result =
          ???
      }

      val result = successful.asResult

      status(result) mustBe Status.ACCEPTED
      contentAsString(result) mustBe aResult.aString
      header("X-Int", result) mustBe Some(aResult.anInt.toString)
    }
  }

  "Failure BusinessTry" should {
    val failure = BusinessTry.failure(Problems.INTERNAL_SERVER_ERROR)

    "a faulure" in {
      failure.awaitResult.isSuccess mustBe false
      failure.awaitResult.isFailure mustBe true
    }

    "map errors" in {
      val BusinessFailure(problem) = failure.mapProblem(p => p.copy(code = 1)).awaitResult
      problem.code must equal(1)
    }

    "not be mapable" in {
      val mapper = mockFunction[String, String]

      mapper.expects(*).never()

      val BusinessFailure(problem) = failure.map(mapper).awaitResult

      problem mustBe Problems.INTERNAL_SERVER_ERROR
    }

    "not be flatMapable" in {
      val mapper = mockFunction[String, BusinessTry[String]]

      mapper.expects(*).never()

      val BusinessFailure(problem) = failure.flatMap(mapper).awaitResult

      problem mustBe Problems.INTERNAL_SERVER_ERROR
    }

    "not be filterable" in {
      val condition  = mockFunction[String, Boolean]
      val aCondition = BusinessCondition(condition, Problems.BAD_REQUEST)

      condition.expects(*).never()

      val BusinessFailure(problem) =
        failure.withCondition(aCondition).awaitResult

      problem mustBe Problems.INTERNAL_SERVER_ERROR
    }

    "fold" in {
      val aNewSuccess = BusinessTry.success("A new success")

      val onSuccess = mockFunction[String, BusinessTry[String]]
      val onFailure = mockFunction[Problem, BusinessTry[String]]

      onSuccess.expects(*).never()
      onFailure.expects(Problems.INTERNAL_SERVER_ERROR).returns(aNewSuccess)

      val BusinessSuccess(result) =
        failure.fold(onSuccess, onFailure).awaitResult

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

    "convert to failed futures" in {
      an[BusinessTryFailedException] must be thrownBy await(
        BusinessTry
          .wrap(Future.successful(()))
          .withCondition(BusinessCondition(_ => false, Problems.BAD_REQUEST))
          .asFutureSuccess)
    }

  }

  "BusinessTry" should {
    "convert to failed futures" in {
      an[BusinessTryFailedException] must be thrownBy await(
        BusinessTry.failure(Problems.BAD_REQUEST).asFutureSuccess)
    }

    "wrap and handle Problems" in {

      val promise = Promise[String]
      val pf: PartialFunction[Throwable, Problem] = {
        case e: Exception => Problems.CONFLICT
      }

      val businessTry = BusinessTry.wrap(promise.future, pf)

      promise.failure(new StringIndexOutOfBoundsException)

      val result = businessTry.awaitResult

      result.isSuccess mustBe false
      result.isFailure mustBe true
      result.asResult(
        new ResultConverter[String] {
          override def onProblem(problem: Problem)(
              implicit loggingContext: LoggingContext): Result = {
            problem mustBe Problems.CONFLICT
            Results.Ok
          }

          override def onFailure(cause: Throwable)(
              implicit loggingContext: LoggingContext): Result =
            fail("Fail is not an expected behaviour")

          override def onSuccess(result: String): Result =
            fail("Success is not an expected behaviour")
        },
        global,
        loggingContext
      )
    }

    "serialize" in {
      def someFutureOp(i: Int): BusinessTry[Int] = BusinessTry.wrap(Future.successful(i + 4))
      val result                                 = BusinessTry.serialize(List(1, 2, 3))(someFutureOp).asFutureSuccess.futureValue
      result mustBe List(5, 6, 7)
    }
  }

  "BusinessTry" should {
    "be compatible with for comprehension" in {
      val firstTry = BusinessTry.success("first result")

      val map1 = mockFunction[String, BusinessTry[String]]
      val map2 = mockFunction[String, BusinessTry[String]]
      val map3 = mockFunction[String, String]

      map1
        .expects("first result")
        .returns(BusinessTry.success("second result"))
      map2
        .expects("second result")
        .returns(BusinessTry.futureSuccess(Future.successful("third result")))
      map3.expects("third result").returns("fourth result")

      val condition  = mockFunction[String, Boolean]
      val aCondition = BusinessCondition(condition, Problems.BAD_REQUEST)

      condition.expects("third result").returns(true)

      val lastTry = for {
        first    <- firstTry
        second   <- map1(first)
        third    <- map2(second)
        filtered <- aCondition(third)
      } yield map3(filtered)

      val BusinessSuccess(result) = lastTry.awaitResult

      result mustEqual "fourth result"
    }
    "be recoverable" in {
      val BusinessSuccess(result) = BusinessTry
        .failure(Problem.forStatus(100, "test"))
        .recoverProblem {
          case Problem(100, _, _, _) => "success"
        }
        .awaitResult

      result must equal("success")
    }

    "should not recover if partial function doesn't match" in {
      val BusinessFailure(result) = BusinessTry
        .failure(Problem.forStatus(100, "test"))
        .recoverProblem {
          case Problem(101, _, _, _) => "success"
        }
        .awaitResult

      result.code must equal(100)
    }

    "recover failures" in {
      def spookyCode: (String) => String = { _ =>
        throw new RuntimeException("Crawoom")
      }

      val BusinessSuccess(result) =
        BusinessTry
          .success("initial")
          .map(spookyCode)
          .recoverFailure {
            case e: RuntimeException if e.getMessage == "Crawoom" =>
              "recovered"
          }
          .awaitResult

      result must equal("recovered")
    }
  }

}
