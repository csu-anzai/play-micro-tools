package microtools.wire

import com.softwaremill.tagging.@@
import com.softwaremill.macwire.wire
import org.scalatest.{MustMatchers, WordSpec}

class WireTagSpec extends WordSpec with MustMatchers {

  "Wiretags" should {
    object Test extends FixedWireTag[String]("test")
    import microtools.wire.WireTag._

    "resolvable with explicit tagging" in {
      class Te(test: String @@ Test.type)

      wire[Te]
    }

    "resolvable with Value type" in {
      class Te(test: Test.Value)

      wire[Te]
    }

    "both methods should be equal" in {
      implicitly[String @@ Test.type =:= Test.Value]
    }
  }

}
