package microtools.models

import play.api.mvc.Headers

case class ServiceName(name: String) extends AnyVal {
  override def toString: String = name
}

case class Scopes(scopes: Set[String]) extends AnyVal {
  def contains(scope: String): Boolean = scopes.contains(scope)
}

object Scopes {
  val empty: Scopes = Scopes(Set.empty[String])

  def apply(scopes: String*): Scopes = Scopes(scopes.toSet)
}

case class ScopesByService(scopesByService: Map[String, Scopes]) extends AnyVal {
  def forService(serviceName: ServiceName): Scopes =
    scopesByService.getOrElse(serviceName.name.toLowerCase, Scopes.empty)
}

object ScopesByService {
  val scopesHeaderPrefix = "x-auth-scopes-"

  def fromHeaders(headers: Headers): ScopesByService = {
    ScopesByService(headers.headers.foldLeft(Map.empty[String, Scopes]) {
      (accu: Map[String, Scopes], header) =>
        val (key, value) = header
        if (key.toLowerCase.startsWith(scopesHeaderPrefix)) {
          val serviceName = key.drop(scopesHeaderPrefix.length).toLowerCase
          accu + (serviceName -> Scopes(accu.getOrElse(serviceName, Scopes.empty).scopes + value))
        } else
          accu
    })
  }
}
