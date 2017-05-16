package microtools.dao

import microtools.logging.{WithContextAwareLogger}
import microtools.models.{ExtraHeaders, RequestContext}
import play.mvc.Http.HeaderNames

@deprecated("Use WSClientWithFlow instead", "0.1-103")
trait ServiceDaoHelper { self: WithContextAwareLogger =>

  def serviceEndpoint: String
  def authScopeHeader: String
  def serviceName: String

  val (serviceUrl, forwardProto) =
    if (serviceEndpoint.startsWith("https:"))
      (s"http:${serviceEndpoint.drop(6)}", "https")
    else
      (serviceEndpoint, "http")

  def logAndThrowError(ex: String => Exception)(errorMessage: String)(
      implicit ctx: RequestContext
  ): Nothing = {
    log.error(errorMessage)
    throw ex(errorMessage)
  }

  @deprecated(
    "handleError() is a lie and misleads people reading your code, please use logAndThrowError() instead which does the same thing and has the same signature",
    "0.1-89"
  )
  def handleError(ex: String => Exception)(errorMessage: String)(
      implicit ctx: RequestContext
  ): Nothing = logAndThrowError(ex)(errorMessage)

  def headers: Seq[(String, String)] =
    Seq(
      ExtraHeaders.AUTH_SUBJECT_HEADER -> s"service/$serviceName",
      authScopeHeader                  -> "R",
      HeaderNames.X_FORWARDED_PROTO    -> forwardProto
    )
}
