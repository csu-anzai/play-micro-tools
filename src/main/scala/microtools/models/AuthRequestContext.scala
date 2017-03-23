package microtools.models

case class Organization(id: String) extends AnyVal {
  override def toString: String = id
}

case class Token(token: String) extends AnyVal {
  override def toString: String = token

  def authorizationHeader: String = s"Bearer $token"
}

trait AuthRequestContext extends RequestContext {
  def subject: Subject

  def organization: Option[Organization]

  def scopes: ScopesByService

  def token: Token
}
