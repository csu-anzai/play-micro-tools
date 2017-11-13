package microtools

import akka.util.Timeout
import microtools.logging.LoggingContext
import microtools.models.{Problem, Problems}
import play.api.libs.json.{JsValue, Reads}
import play.api.mvc.Result

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class BusinessTryFailedException(problem: Problem) extends RuntimeException(problem.toString)

/**
  * BusinnesTry is a useful variant of `scala.util.Try` and `scala.concurrent.Future`.
  * The main idea is, that a `BusinessTry` has three states: Successful, Failure/Problem and Exception.
  * A `Problem` can be considered as any kind of known or expected exception, i.e. a to be expected business error,
  * whereas `Exception` may be anything unexpected, i.e. a technical error.
  *
  * The other way round, you may argue that a `Problem` is part of your business logic and thereby reproducable, i.e.
  * if your system get's the same input in the same state the same `Problem` will occur over and over again. Whereas an
  * `Exception` is generally not reproducible and might fix itself after some time (e.g. a temporary network outage).
  *
  * In general a `BusinessTry` is undecided, i.e. it might be a success or failure in the future.
  *
  * @tparam R the result type of the business try
  */
sealed trait BusinessTry[+R] {

  /**
    * Await the outcome of the business try.
    * The will either create a `DecidedBusinessTry` or throw an `Exception`.
    * Usfull for testing, should be used with care in production code as the thread will be blocked.
    *
    * @param timeout timeout for the await, will throw a TimeoutException is exceeded
    * @return `DecidedBusinessTry`
    */
  def awaitResult(implicit timeout: Timeout): DecidedBusinessTry[R]

  /**
    * Convert the `BusinessTry` to an asynchronous action result.
    * @param converter converts the three final states of a BusinessTry to Play Result
    * @return the returned future will not fail
    */
  def asResult(implicit converter: ResultConverter[R],
               ec: ExecutionContext,
               loggingContext: LoggingContext): Future[Result]

  /**
    * Convert the `BusinessTry` to an asynchronous action result.
    *
    * Is the same as `this.asActionResult(converter) === this.asResult(converter, implicitly[ExecutionContext])`
    * @param converter converts the three final states of a BusinessTry to Play Result
    * @return the returned future will not fail
    */
  def asActionResult(converter: ResultConverter[R])(
      implicit ec: ExecutionContext,
      loggingContext: LoggingContext
  ): Future[Result] =
    asResult(converter, ec, loggingContext)

  def asFuture(implicit ec: ExecutionContext): Future[Either[R, Problem]]

  def asFutureSuccess(implicit ec: ExecutionContext): Future[R]

  def mapProblem(f: Problem => Problem)(implicit ec: ExecutionContext): BusinessTry[R]

  def map[U](f: R => U)(implicit ec: ExecutionContext): BusinessTry[U]

  def flatMap[U](f: R => BusinessTry[U])(implicit ec: ExecutionContext): BusinessTry[U]

  def withCondition(condition: BusinessCondition[R])(implicit ec: ExecutionContext): BusinessTry[R]

  def fold[U](onSuccess: R => BusinessTry[U], onFailure: Problem => BusinessTry[U])(
      implicit ec: ExecutionContext
  ): BusinessTry[U]

  def onComplete(callback: Try[DecidedBusinessTry[R]] => Unit)(
      implicit ec: ExecutionContext
  ): BusinessTry[R]

  def zip[U](that: BusinessTry[U])(implicit ec: ExecutionContext): BusinessTry[(R, U)] = {
    flatMap { thisResult =>
      that.map { thatResult =>
        (thisResult, thatResult)
      }
    }
  }

  def recoverWith[U >: R](f: PartialFunction[Problem, BusinessTry[U]])(
      implicit ec: ExecutionContext
  ): BusinessTry[U]

  def recover[U >: R](f: PartialFunction[Problem, U])(
      implicit ec: ExecutionContext
  ): BusinessTry[U] = {
    recoverWith(f.andThen(BusinessTry.success))
  }
}

/**
  * A `DecidedBusinessTry` is a `BusinessTry` with a concrete outcome: Success or Failure.
  * There is no `Exception` case any more, since any kind of exception has been already thrown at this point and had
  * to be dealt with in the usual manner (in most cases resulting in a HTTP 500).
  *
  * @tparam R the result type of the business try
  */
sealed trait DecidedBusinessTry[+R] extends BusinessTry[R] {
  def isSuccess: Boolean

  def isFailure: Boolean

  override def onComplete(callback: Try[DecidedBusinessTry[R]] => Unit)(
      implicit ec: ExecutionContext
  ): BusinessTry[R] = {
    Future {
      callback(Success(this))
    }
    this
  }
}

object BusinessTry {
  def success[R](result: R): BusinessTry[R] = BusinessSuccess(result)

  def failure(problem: Problem): BusinessTry[Nothing] =
    BusinessFailure(problem)

  def futureSuccess[R](futureResult: Future[R])(implicit ec: ExecutionContext): BusinessTry[R] =
    FutureBusinessTry(futureResult.map(success).recover {
      case e: ProblemException => BusinessTry.failure(e.problem)
    })

  def future[R](futureTry: Future[BusinessTry[R]])(implicit ec: ExecutionContext): BusinessTry[R] =
    FutureBusinessTry(futureTry.recover {
      case e: ProblemException => BusinessTry.failure(e.problem)
    })

  def wrap[R](
      futureResult: Future[R],
      handleProblem: PartialFunction[Throwable, Problem] = PartialFunction.empty
  )(
      implicit ec: ExecutionContext
  ): BusinessTry[R] =
    FutureBusinessTry(futureResult.map(success).recover(handleProblem.andThen(failure)))

  def sequence[U](tries: TraversableOnce[BusinessTry[U]])(
      implicit ec: ExecutionContext
  ): BusinessTry[Seq[U]] =
    tries.foldLeft[BusinessTry[Seq[U]]](success(Seq.empty)) { (results, aTry) =>
      results.flatMap(rs => aTry.map(result => rs :+ result))
    }

  @deprecated("Use the more common name `sequence` of this operation", since = "0.1-128")
  def forAll[U](tries: TraversableOnce[BusinessTry[U]])(
      implicit ec: ExecutionContext): BusinessTry[Seq[U]] = sequence(tries)

  def validateJson[T](json: JsValue)(implicit reads: Reads[T]): DecidedBusinessTry[T] = {
    json.validate.fold(
      jsonErrors => BusinessFailure(Problems.jsonValidationErrors(jsonErrors)),
      BusinessSuccess(_)
    )
  }

  def transformJson(
      json: JsValue,
      transformation: Reads[_ <: JsValue]
  ): DecidedBusinessTry[JsValue] = {
    json
      .transform(transformation)
      .fold(
        jsonErrors => BusinessFailure(Problems.jsonTransformErrors(jsonErrors)),
        BusinessSuccess(_)
      )
  }

  def require[R](option: Option[R], problem: Problem): BusinessTry[R] =
    option match {
      case Some(value) => success(value)
      case None        => failure(problem)
    }

  def fromTry[T](`try`: Try[T])(errorHandler: Throwable => Problem): BusinessTry[T] = {
    `try` match {
      case Success(t) => BusinessTry.success(t)
      case Failure(e) => BusinessTry.failure(errorHandler(e))
    }
  }
}

case class BusinessSuccess[R](result: R) extends DecidedBusinessTry[R] {
  override def isSuccess: Boolean = true

  override def isFailure: Boolean = false

  override def asResult(implicit converter: ResultConverter[R],
                        ec: ExecutionContext,
                        loggingContext: LoggingContext): Future[Result] =
    Future.successful(converter.onSuccess(result))

  override def asFuture(implicit ec: ExecutionContext): Future[Either[R, Problem]] =
    Future.successful(Left(result))

  override def asFutureSuccess(implicit ec: ExecutionContext): Future[R] =
    Future.successful(result)

  override def mapProblem(f: Problem => Problem)(implicit ec: ExecutionContext): BusinessTry[R] =
    this

  override def map[U](f: (R) => U)(implicit ec: ExecutionContext): BusinessTry[U] =
    BusinessSuccess(f(result))

  override def flatMap[U](f: (R) => BusinessTry[U])(
      implicit ec: ExecutionContext
  ): BusinessTry[U] = f(result)

  override def withCondition(condition: BusinessCondition[R])(
      implicit ec: ExecutionContext
  ): BusinessTry[R] = condition(result)

  override def fold[U](onSuccess: (R) => BusinessTry[U], onFailure: (Problem) => BusinessTry[U])(
      implicit ec: ExecutionContext
  ): BusinessTry[U] =
    onSuccess(result)

  override def awaitResult(implicit timeout: Timeout): DecidedBusinessTry[R] =
    this

  override def recoverWith[U >: R](f: PartialFunction[Problem, BusinessTry[U]])(
      implicit ec: ExecutionContext
  ): BusinessTry[U] =
    this
}

case class BusinessFailure(problem: Problem) extends DecidedBusinessTry[Nothing] {
  override def isSuccess: Boolean = false

  override def isFailure: Boolean = true

  override def asResult(implicit converter: ResultConverter[Nothing],
                        ec: ExecutionContext,
                        loggingContext: LoggingContext): Future[Result] =
    Future.successful(converter.onProblem(problem))

  override def asFuture(implicit ec: ExecutionContext): Future[Either[Nothing, Problem]] =
    Future.successful(Right(problem))

  override def asFutureSuccess(implicit ec: ExecutionContext): Future[Nothing] =
    Future.failed(new BusinessTryFailedException(problem))

  override def mapProblem(f: Problem => Problem)(
      implicit ec: ExecutionContext): BusinessTry[Nothing] =
    BusinessFailure(f(problem))

  override def map[U](f: (Nothing) => U)(implicit ec: ExecutionContext): BusinessTry[U] =
    this

  override def flatMap[U](f: (Nothing) => BusinessTry[U])(
      implicit ec: ExecutionContext
  ): BusinessTry[U] =
    this

  override def withCondition(condition: BusinessCondition[Nothing])(
      implicit ec: ExecutionContext
  ): BusinessTry[Nothing] = this

  override def fold[U](onSuccess: (Nothing) => BusinessTry[U],
                       onFailure: (Problem) => BusinessTry[U])(
      implicit ec: ExecutionContext
  ): BusinessTry[U] =
    onFailure(problem)

  override def awaitResult(implicit timeout: Timeout): DecidedBusinessTry[Nothing] =
    this

  override def recoverWith[U >: Nothing](f: PartialFunction[Problem, BusinessTry[U]])(
      implicit ec: ExecutionContext
  ): BusinessTry[U] =
    if (f isDefinedAt problem) f(problem) else this

}

case class FutureBusinessTry[R](futureTry: Future[BusinessTry[R]]) extends BusinessTry[R] {
  override def asResult(implicit converter: ResultConverter[R],
                        ec: ExecutionContext,
                        loggingContext: LoggingContext): Future[Result] =
    futureTry.flatMap(_.asResult)

  override def asFuture(implicit ec: ExecutionContext): Future[Either[R, Problem]] =
    futureTry.flatMap(_.asFuture)

  override def asFutureSuccess(implicit ec: ExecutionContext): Future[R] =
    futureTry.flatMap(_.asFutureSuccess)

  override def mapProblem(f: Problem => Problem)(implicit ec: ExecutionContext): BusinessTry[R] =
    FutureBusinessTry[R](futureTry.map(_.mapProblem(f)))

  override def map[U](f: (R) => U)(implicit ec: ExecutionContext): BusinessTry[U] =
    FutureBusinessTry[U](futureTry.map(_.map(f)))

  override def flatMap[U](f: (R) => BusinessTry[U])(
      implicit ec: ExecutionContext
  ): BusinessTry[U] =
    FutureBusinessTry[U](futureTry.map(_.flatMap(f)))

  override def withCondition(condition: BusinessCondition[R])(
      implicit ec: ExecutionContext
  ): BusinessTry[R] =
    FutureBusinessTry[R](futureTry.map(_.withCondition(condition)))

  override def fold[U](onSuccess: (R) => BusinessTry[U], onFailure: (Problem) => BusinessTry[U])(
      implicit ec: ExecutionContext
  ): BusinessTry[U] =
    FutureBusinessTry(futureTry.map(_.fold(onSuccess, onFailure)))

  override def awaitResult(implicit timeout: Timeout): DecidedBusinessTry[R] =
    Await.result(futureTry, timeout.duration).awaitResult

  override def onComplete(callback: Try[DecidedBusinessTry[R]] => Unit)(
      implicit ec: ExecutionContext
  ): BusinessTry[R] = {
    futureTry.onComplete {
      case Success(businessTry) => businessTry.onComplete(callback)
      case Failure(exception)   => callback(Failure(exception))
    }
    this
  }

  override def recoverWith[U >: R](f: PartialFunction[Problem, BusinessTry[U]])(
      implicit ec: ExecutionContext
  ): BusinessTry[U] =
    FutureBusinessTry(futureTry.map(_.recoverWith(f)))

}
