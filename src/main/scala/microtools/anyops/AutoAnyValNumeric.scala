package microtools.anyops

import _root_.shapeless.{Generic, ::, HNil, HList}

object AutoAnyValNumeric {

  class AnyValFractional[S, T <: AnyVal](anyValApply: S => T)(anyValUnapply: T => S)(
      implicit fractional: Fractional[S])
      extends AnyValNumeric[S, T](anyValApply)(anyValUnapply)
      with Fractional[T] {

    override def div(x: T, y: T): T = applyTwoArgs(x, y, fractional.div)
  }

  class AnyValNumeric[S, T <: AnyVal](anyValApply: S => T)(anyValUnapply: T => S)(
      implicit numeric: Numeric[S])
      extends Numeric[T] {

    def applyTwoArgs(a: T, b: T, op: (S, S) => S): T = {
      anyValApply(op(anyValUnapply(a), anyValUnapply(b)))
    }

    def applyOneArg(a: T, op: (S) => S): T = {
      anyValApply(op(anyValUnapply(a)))
    }

    override def fromInt(x: Int): T                  = anyValApply(numeric.fromInt(x))
    override def minus(x: T, y: T): T                = applyTwoArgs(x, y, numeric.minus)
    override def negate(x: T): T                     = applyOneArg(x, numeric.negate)
    override def plus(x: T, y: T): T                 = applyTwoArgs(x, y, numeric.plus)
    override def times(x: T, y: T): T                = applyTwoArgs(x, y, numeric.times)
    override def toDouble(x: T): Double              = numeric.toDouble(anyValUnapply(x))
    override def toFloat(x: T): Float                = numeric.toFloat(anyValUnapply(x))
    override def toInt(x: T): Int                    = numeric.toInt(anyValUnapply(x))
    override def toLong(x: T): Long                  = numeric.toLong(anyValUnapply(x))
    override def compare(x: T, y: T): Int            = numeric.compare(anyValUnapply(x), anyValUnapply(y))
    override def parseString(str: String): Option[T] = numeric.parseString(str).map(anyValApply)

  }

  implicit def numericAnyVal[T <: AnyVal, L <: HList, S](implicit gen: Generic.Aux[T, L],
                                                         ev1: (S :: HNil) =:= L,
                                                         ev2: L =:= (S :: HNil),
                                                         format: Numeric[S]): Numeric[T] =
    new AnyValNumeric[S, T](s => gen.from(ev1(s :: HNil)))(t => gen.to(t).head)

  implicit def fractionalAnyVal[T <: AnyVal, L <: HList, S](implicit gen: Generic.Aux[T, L],
                                                            ev1: (S :: HNil) =:= L,
                                                            ev2: L =:= (S :: HNil),
                                                            format: Fractional[S]): Fractional[T] =
    new AnyValFractional[S, T](s => gen.from(ev1(s :: HNil)))(t => gen.to(t).head)
}
