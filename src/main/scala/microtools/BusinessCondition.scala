package microtools

import microtools.models.Problem

case class BusinessCondition[-T](condition: T => Boolean, problem: Problem) {
  def apply[R <: T](value: R): BusinessTry[R] =
    if (condition(value)) BusinessTry.success(value)
    else BusinessTry.failure(problem)
}
