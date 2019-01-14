package microtools.models

case class ForwardedHost(value: Option[String]) extends AnyVal {
  def domain: Option[String] = value.flatMap { v =>
    v.split("\\.") match {
      case Array(_, domain, tld) => Some(s"$domain.$tld")
      case _                     => None
    }
  }
}
