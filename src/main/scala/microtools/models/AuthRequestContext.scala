package microtools.models

sealed trait Organization extends Any

case object NoOrganization extends Organization

case class GenericOrganization(id: String) extends AnyVal with Organization {
  override def toString: String = id
}

object Organization {
  def apply(org: Option[String]): Organization = org match {
    case Some(id) => GenericOrganization(id)
    case None     => NoOrganization
  }
}

case class Token(token: String) extends AnyVal {
  override def toString: String = token

  def authorizationHeader: String = s"Bearer $token"
}

trait AuthRequestContext extends RequestContext {
  def subject: Subject

  def organization: Organization

  def scopes: ScopesByService

  def token: Token
}
