package microtools.models

import microtools.wire.WireTag

import scala.annotation.implicitNotFound

@implicitNotFound(
  "No ServiceName found. Try to implement an object FixedWireTag[ServiceName](\"<your service name>\") and import its .self into your scope.")
case class ServiceName(name: String) extends AnyVal {
  override def toString: String = name
}

object ServiceName {
  implicit def givenServiceName[TAG <: WireTag[ServiceName]](implicit tag: TAG): ServiceName =
    ServiceName(tag.default)
}
