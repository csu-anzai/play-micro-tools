package microtools.models

import play.api.libs.json._

sealed trait Organization extends Any {
  def maybeId: Option[String]
}

case object NoOrganization extends Organization {
  override def maybeId: Option[String] = None

  override def toString: String = "<no organization>"
}

case class GenericOrganization(id: String) extends AnyVal with Organization {
  override def maybeId: Option[String] = Some(id)
  override def toString: String        = id
}

object Organization {
  def apply(org: Option[String]): Organization = org match {
    case Some(id) => GenericOrganization(id)
    case None     => NoOrganization
  }

  implicit val jsonReads: Reads[Organization] = __.readNullable[String].map(apply)

  implicit val jsonWrites: Writes[Organization] =
    Writes[Organization](_.maybeId.map(JsString).getOrElse(JsNull))
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
