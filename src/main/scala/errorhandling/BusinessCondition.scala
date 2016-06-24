package errorhandling

import errorhandling.models.Problem

case class BusinessCondition[-T](condition: T => Boolean, problem: Problem)
