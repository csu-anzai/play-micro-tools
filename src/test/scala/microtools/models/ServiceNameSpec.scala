package microtools.models

import microtools.wire.FixedWireTag
import org.scalatest.{FlatSpec, Matchers}

class ServiceNameSpec extends FlatSpec with Matchers {

  "Service name" should "get it's name by WireTag" in {
    val givenServiceName = "TestServiceName"

    implicit object MyServiceName extends FixedWireTag[ServiceName](givenServiceName)

    ServiceName().name shouldBe givenServiceName
  }

  it should "not compile without a defined WireTag" in {
    "ServiceName()" shouldNot compile
  }
}
