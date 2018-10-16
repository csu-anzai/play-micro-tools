package microtools.anyops

import org.scalatest.{MustMatchers, WordSpec}
import microtools.anyops.AutoAnyValNumeric._
import scala.math.Numeric.Implicits._
import scala.math.Fractional.Implicits._

case class Bauer(value: Int)    extends AnyVal
case class Ralph(value: Double) extends AnyVal

class AnuValNumericOperationsSpec extends WordSpec with MustMatchers {
  "AnyVal operations spec" should {
    "apply operators on anyval" in {
      Bauer(2) + Bauer(3) must equal(Bauer(5))
      Bauer(2) - Bauer(3) must equal(Bauer(-1))
      Bauer(2) * Bauer(3) must equal(Bauer(6))
      Ralph(10.0) / Ralph(2.0d) must equal(Ralph(5.0d))

      Bauer(2).toInt must equal(2)
      Bauer(2).toDouble must equal(2.0d)
      Bauer(2).toFloat must equal(2.0f)
      Bauer(2).toLong must equal(2L)

      -Bauer(2) must equal(Bauer(-2))
      Bauer(2) must be <= Bauer(3)
      Bauer(3) must be >= Bauer(2)
      Bauer(2) must be < Bauer(3)
      Bauer(3) must be > Bauer(2)

    }
  }
}
