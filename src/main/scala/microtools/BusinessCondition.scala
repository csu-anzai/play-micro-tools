package microtools

import microtools.models.Problem
import scala.concurrent.ExecutionContext.Implicits.global

trait BusinessCondition[-T] {
  def apply[R <: T](value: R): BusinessTry[R]
}

object BusinessCondition {
  def identity[T]: BusinessCondition[T] =
    new BusinessCondition[T] {
      override def apply[R <: T](value: R): BusinessTry[R] =
        BusinessTry.success(value)
    }
  def nope[T](problem: Problem): BusinessCondition[T] =
    new BusinessCondition[T] {
      override def apply[R <: T](value: R): BusinessTry[R] =
        BusinessTry.failure(problem)
    }

  def apply[T](condition: T => Boolean, problem: Problem): BusinessCondition[T] =
    new BusinessCondition[T] {
      override def apply[R <: T](value: R): BusinessTry[R] =
        if (condition(value)) BusinessTry.success(value)
        else BusinessTry.failure(problem)
    }

  def and[T](left: BusinessCondition[T], right: BusinessCondition[T]): BusinessCondition[T] =
    new BusinessCondition[T] {
      override def apply[R <: T](value: R): BusinessTry[R] =
        for {
          l <- left(value)
          r <- right(value)
        } yield r
    }

  def or[T](left: BusinessCondition[T], right: BusinessCondition[T]): BusinessCondition[T] =
    new BusinessCondition[T] {
      override def apply[R <: T](value: R): BusinessTry[R] =
        left(value).fold(l => BusinessTry.success(l), p => right(value))
    }

  def all[T](conditions: Seq[BusinessCondition[T]]): BusinessCondition[T] =
    new BusinessCondition[T] {
      override def apply[R <: T](value: R): BusinessTry[R] =
        BusinessTry.forAll(conditions.map(_.apply(value))).map(_ => value)
    }

  implicit class BusinessConditionCombinators[T](left: BusinessCondition[T]) {
    def and(right: BusinessCondition[T]): BusinessCondition[T] = BusinessCondition.and(left, right)

    def or(right: BusinessCondition[T]): BusinessCondition[T] = BusinessCondition.or(left, right)
  }
}
