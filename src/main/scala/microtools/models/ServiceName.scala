package microtools.models

import microtools.wire.WireTag

import scala.annotation.implicitNotFound

@implicitNotFound(
  "No service name found. Try to implement an implicit object WireTag[ServiceName](\"<your service name>\") and import it into your scope.")
case class ServiceName(name: String) extends AnyVal {
  override def toString: String = name
}

object ServiceName {
  def apply[TAG <: WireTag[ServiceName]]()(implicit tag: TAG): ServiceName =
    ServiceName(tag.default)

  implicit def givenServiceName[TAG <: WireTag[ServiceName]](implicit tag: TAG): ServiceName =
    ServiceName(tag.default)
}
