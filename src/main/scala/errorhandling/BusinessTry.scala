package errorhandling

import akka.util.Timeout
import errorhandling.models.{Problem, Problems}
import play.api.libs.json.{JsValue, Reads, Writes}
import play.api.mvc.Result

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * BusinnesTry is a useful variant of `scala.util.Try` and `scala.concurrent.Future`.
  * The main idea is, that a `BusinessTry` has three states: Successful, Failure/Problem and Exception.
  * A `Problem` can be considered as any kind of known or expected exception, i.e. a to be expected business error,
  * whereas `Exception` may be anything unexpected, i.e. a technical error.
  *
  * The other wy round, you may argue that a `Problem` is part of your business logic and thereby reproducable, i.e.
  * if your system get's the same input the same `Problem` will occure over and over again. Whereas an `Exception` is
  * generally not reproducable and might ifx itself after some time (e.g. a temporary network outage).
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
    */
  def asResult(implicit converter: ResultConverter[R],
               ec: ExecutionContext): Future[Result]

  def map[U](f: R => U)(implicit ec: ExecutionContext): BusinessTry[U]

  def flatMap[U](f: R => BusinessTry[U])(
      implicit ec: ExecutionContext): BusinessTry[U]

  def withCondition(condition: BusinessCondition[R])(
      implicit ec: ExecutionContext): BusinessTry[R]

  def fold[U](
      onSuccess: R => BusinessTry[U], onFailure: Problem => BusinessTry[U])(
      implicit ec: ExecutionContext): BusinessTry[U]

  def onComplete(callback: Try[DecidedBusinessTry[R]] => Unit)(
      implicit ec: ExecutionContext): BusinessTry[R]

  def zip[U](that: BusinessTry[U])(
      implicit ec: ExecutionContext): BusinessTry[(R, U)] = {
    flatMap { thisResult =>
      that.map { thatResult =>
        (thisResult, thatResult)
      }
    }
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
      implicit ec: ExecutionContext): BusinessTry[R] = {
    Future {
      callback(Success(this))
    }
    this
  }
}

object BusinessTry {
  def success[R](result: R): DecidedBusinessTry[R] = BusinessSuccess(result)

  def failure[R](problem: Problem): DecidedBusinessTry[R] =
    BusinessFailure[R](problem)

  def futureSuccess[R](futureResult: Future[R])(
      implicit ec: ExecutionContext): BusinessTry[R] =
    FutureBusinessTry(futureResult.map(success))

  def future[R](futureTry: Future[BusinessTry[R]]): BusinessTry[R] =
    FutureBusinessTry(futureTry)

  def forAll[U](tries: TraversableOnce[BusinessTry[U]])(
      implicit ec: ExecutionContext): BusinessTry[Seq[U]] =
    tries.foldLeft[BusinessTry[Seq[U]]](BusinessTry.success(Seq.empty)) {
      (results, aTry) =>
        results.flatMap(rs => aTry.map(result => rs :+ result))
    }

  def validateJson[T](json: JsValue)(
      implicit reads: Reads[T]): DecidedBusinessTry[T] = {
    json.validate.fold(
        jsonErrors => failure(Problems.jsonValidationErrors(jsonErrors)),
        success
    )
  }
}

case class BusinessSuccess[R](result: R) extends DecidedBusinessTry[R] {
  override def isSuccess: Boolean = true

  override def isFailure: Boolean = false

  override def asResult(implicit converter: ResultConverter[R],
                        ec: ExecutionContext): Future[Result] =
    Future.successful(converter.onSuccess(result))

  override def map[U](f: (R) => U)(
      implicit ec: ExecutionContext): BusinessTry[U] =
    BusinessSuccess(f(result))

  override def flatMap[U](f: (R) => BusinessTry[U])(
      implicit ec: ExecutionContext): BusinessTry[U] = f(result)

  override def withCondition(condition: BusinessCondition[R])(
      implicit ec: ExecutionContext): BusinessTry[R] =
    if (condition.condition(result))
      this
    else
      BusinessFailure[R](condition.problem)

  override def fold[U](onSuccess: (R) => BusinessTry[U],
                       onFailure: (Problem) => BusinessTry[U])(
      implicit ec: ExecutionContext): BusinessTry[U] =
    onSuccess(result)

  override def awaitResult(implicit timeout: Timeout): DecidedBusinessTry[R] =
    this
}

case class BusinessFailure[R](problem: Problem) extends DecidedBusinessTry[R] {
  override def isSuccess: Boolean = false

  override def isFailure: Boolean = true

  override def asResult(implicit converter: ResultConverter[R],
                        ec: ExecutionContext): Future[Result] =
    Future.successful(converter.onProblem(problem))

  override def map[U](f: (R) => U)(
      implicit ec: ExecutionContext): BusinessTry[U] =
    this.asInstanceOf[BusinessTry[U]]

  override def flatMap[U](f: (R) => BusinessTry[U])(
      implicit ec: ExecutionContext): BusinessTry[U] =
    this.asInstanceOf[BusinessTry[U]]

  override def withCondition(condition: BusinessCondition[R])(
      implicit ec: ExecutionContext): BusinessTry[R] = this

  override def fold[U](onSuccess: (R) => BusinessTry[U],
                       onFailure: (Problem) => BusinessTry[U])(
      implicit ec: ExecutionContext): BusinessTry[U] =
    onFailure(problem)

  override def awaitResult(implicit timeout: Timeout): DecidedBusinessTry[R] =
    this
}

case class FutureBusinessTry[R](futureTry: Future[BusinessTry[R]])
    extends BusinessTry[R] {
  override def asResult(implicit converter: ResultConverter[R],
                        ec: ExecutionContext): Future[Result] =
    futureTry.flatMap(_.asResult).recover {
      case cause: Throwable =>
        converter.onFailure(cause)
    }

  override def map[U](f: (R) => U)(
      implicit ec: ExecutionContext): BusinessTry[U] =
    FutureBusinessTry[U](futureTry.map(_.map(f)))

  override def flatMap[U](f: (R) => BusinessTry[U])(
      implicit ec: ExecutionContext): BusinessTry[U] =
    FutureBusinessTry[U](futureTry.map(_.flatMap(f)))

  override def withCondition(condition: BusinessCondition[R])(
      implicit ec: ExecutionContext): BusinessTry[R] =
    FutureBusinessTry[R](futureTry.map(_.withCondition(condition)))

  override def fold[U](onSuccess: (R) => BusinessTry[U],
                       onFailure: (Problem) => BusinessTry[U])(
      implicit ec: ExecutionContext): BusinessTry[U] =
    FutureBusinessTry(futureTry.map(_.fold(onSuccess, onFailure)))

  override def awaitResult(implicit timeout: Timeout): DecidedBusinessTry[R] =
    Await.result(futureTry, timeout.duration).awaitResult

  override def onComplete(callback: Try[DecidedBusinessTry[R]] => Unit)(
      implicit ec: ExecutionContext): BusinessTry[R] = {
    futureTry.onComplete {
      case Success(businessTry) => businessTry.onComplete(callback)
      case Failure(exception)   => callback(Failure(exception))
    }
    this
  }
}
