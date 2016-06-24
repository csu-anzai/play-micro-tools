package errorhandling

import akka.util.Timeout
import errorhandling.models.{Problem, Problems}
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import play.api.mvc.Result

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import play.api.mvc.Results._

sealed trait BusinessTry[+R] {
  def awaitResult(implicit timeout: Timeout): DecidedBusinessTry[R]

  def asResult(implicit writes: Writes[R]): Future[Result]

  def map[U](f: R => U): BusinessTry[U]

  def flatMap[U](f: R => BusinessTry[U]): BusinessTry[U]

  def withFilter(condition: BusinessCondition[R]): BusinessTry[R]

  def fold[U](onSuccess: R => BusinessTry[U],
              onFailure: Problem => BusinessTry[U]): BusinessTry[U]

  def onComplete(callback: Try[DecidedBusinessTry[R]] => Unit): BusinessTry[R]

  def zip[U](that: BusinessTry[U]): BusinessTry[(R, U)] = {
    flatMap { thisResult =>
      that.map { thatResult =>
        (thisResult, thatResult)
      }
    }
  }
}

sealed trait DecidedBusinessTry[+R] extends BusinessTry[R] {
  def isSuccess: Boolean

  def isFailure: Boolean

  override def onComplete(
      callback: Try[DecidedBusinessTry[R]] => Unit): BusinessTry[R] = {
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

  def futureSuccess[R](futureResult: Future[R]): BusinessTry[R] =
    FutureBusinessTry(futureResult.map(success))

  def future[R](futureTry: Future[BusinessTry[R]]): BusinessTry[R] =
    FutureBusinessTry(futureTry)

  def forAll[U](tries: TraversableOnce[BusinessTry[U]]): BusinessTry[Seq[U]] =
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

  override def asResult(implicit writes: Writes[R]): Future[Result] = result match {
    case businessResult : BusinessResult => Future.successful(businessResult.asResult)
    case other => Future.successful(Ok(Json.toJson(result)))
  }

  override def map[U](f: (R) => U): BusinessTry[U] = BusinessSuccess(f(result))

  override def flatMap[U](f: (R) => BusinessTry[U]): BusinessTry[U] = f(result)

  override def withFilter(condition: BusinessCondition[R]): BusinessTry[R] =
    if (condition.condition(result))
      this
    else
      BusinessFailure[R](condition.problem)

  override def fold[U](
      onSuccess: (R) => BusinessTry[U],
      onFailure: (Problem) => BusinessTry[U]): BusinessTry[U] =
    onSuccess(result)

  override def awaitResult(implicit timeout: Timeout): DecidedBusinessTry[R] =
    this
}

case class BusinessFailure[R](problem: Problem) extends DecidedBusinessTry[R] {
  override def isSuccess: Boolean = false

  override def isFailure: Boolean = true

  override def asResult(implicit writes: Writes[R]): Future[Result] = Future.successful(problem.asResult)

  override def map[U](f: (R) => U): BusinessTry[U] =
    this.asInstanceOf[BusinessTry[U]]

  override def flatMap[U](f: (R) => BusinessTry[U]): BusinessTry[U] =
    this.asInstanceOf[BusinessTry[U]]

  override def withFilter(condition: BusinessCondition[R]): BusinessTry[R] = this

  override def fold[U](
      onSuccess: (R) => BusinessTry[U],
      onFailure: (Problem) => BusinessTry[U]): BusinessTry[U] =
    onFailure(problem)

  override def awaitResult(implicit timeout: Timeout): DecidedBusinessTry[R] =
    this
}

case class FutureBusinessTry[R](futureTry: Future[BusinessTry[R]])
    extends BusinessTry[R] {
  override def asResult(implicit writes: Writes[R]): Future[Result] = futureTry.flatMap(_.asResult)

  override def map[U](f: (R) => U): BusinessTry[U] =
    FutureBusinessTry[U](futureTry.map(_.map(f)))

  override def flatMap[U](f: (R) => BusinessTry[U]): BusinessTry[U] =
    FutureBusinessTry[U](futureTry.map(_.flatMap(f)))

  override def withFilter(condition: BusinessCondition[R]): BusinessTry[R] =
    FutureBusinessTry[R](futureTry.map(_.withFilter(condition)))

  override def fold[U](
      onSuccess: (R) => BusinessTry[U],
      onFailure: (Problem) => BusinessTry[U]): BusinessTry[U] =
    FutureBusinessTry(futureTry.map(_.fold(onSuccess, onFailure)))

  override def awaitResult(implicit timeout: Timeout): DecidedBusinessTry[R] =
    Await.result(futureTry, timeout.duration).awaitResult

  override def onComplete(
      callback: Try[DecidedBusinessTry[R]] => Unit): BusinessTry[R] = {
    futureTry.onComplete {
      case Success(businessTry) => businessTry.onComplete(callback)
      case Failure(exception)   => callback(Failure(exception))
    }
    this
  }
}
