package microtools.models

import microtools.wire.FixedWireTag
import org.scalatest.{FlatSpec, Matchers}

class ServiceNameSpec extends FlatSpec with Matchers {

  "Service name" should "get it's name by WireTag" in {
    val givenServiceName = "TestServiceName"

    object MyServiceName extends FixedWireTag[ServiceName](givenServiceName)

    import MyServiceName.self

    implicitly[ServiceName].name shouldBe givenServiceName
  }

  it should "not compile without a defined WireTag" in {
    "implicitly[ServiceName]" shouldNot compile
  }
}
