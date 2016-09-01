package microtools.models

trait AuthRequestContext extends RequestContext {
  def subject: String

  def scopes: Map[String, Seq[String]]

  def token: String
}
