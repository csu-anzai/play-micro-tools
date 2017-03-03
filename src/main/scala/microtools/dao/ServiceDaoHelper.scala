package microtools.dao

import microtools.models.ExtraHeaders
import play.api.Logger
import play.mvc.Http.HeaderNames

trait ServiceDaoHelper {

  def serviceEndpoint: String
  def authScopeHeader: String
  def serviceName: String

  val (serviceUrl, forwardProto) =
    if (serviceEndpoint.startsWith("https:"))
      (s"http:${serviceEndpoint.drop(6)}", "https")
    else
      (serviceEndpoint, "http")

  def handleError(ex: String => Exception)(errorMessage: String): Nothing = {
    Logger.error(errorMessage)
    throw ex(errorMessage)
  }

  def headers: Seq[(String, String)] =
    Seq(ExtraHeaders.AUTH_SUBJECT_HEADER -> s"service/$serviceName",
      authScopeHeader                  -> "R",
      HeaderNames.X_FORWARDED_PROTO -> forwardProto)
}
