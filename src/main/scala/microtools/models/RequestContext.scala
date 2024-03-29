package microtools.models

import java.util.UUID

import microtools.logging.LoggingContext
import play.api.mvc.RequestHeader
import play.mvc.Http.HeaderNames

import scala.util.Try

trait RequestContext extends LoggingContext {
  def flowId: String

  def ipAddress: String

  def userAgent: Option[String]

  def organization: Organization

  def maybeSubject: Option[Subject]

  def forwardedHost: ForwardedHost
}

object RequestContext {
  def static(staticFlowId: String, _contextValues: (String, String)*): RequestContext =
    new RequestContext {
      override def flowId: String = staticFlowId

      override lazy val contextValues: Seq[(String, String)] = Seq(
        "flow_id" -> flowId
      ) ++ _contextValues

      override def enableBusinessDebug: Boolean = false

      override def ipAddress = ""

      override def userAgent = None

      override def organization = NoOrganization

      override def maybeSubject = None

      override def forwardedHost = ForwardedHost(value = None)
    }

  def forRequest(request: RequestHeader): RequestContext = new RequestContext {
    override def flowId: String =
      request.cookies
        .get(ExtraHeaders.FLOW_ID_HEADER)
        .map(_.value)
        .getOrElse(
          request.headers
            .get(ExtraHeaders.FLOW_ID_HEADER)
            .getOrElse(UUID.randomUUID().toString)
        )

    override def contextValues: Seq[(String, String)] = Seq(
      "flow_id"     -> flowId,
      "request_uri" -> request.uri
    )

    override def enableBusinessDebug: Boolean =
      request.cookies
        .get(ExtraHeaders.DEBUG_HEADER)
        .flatMap(c => Try(c.value.toBoolean).toOption)
        .getOrElse(
          request.headers
            .get(ExtraHeaders.DEBUG_HEADER)
            .flatMap(s => Try(s.toBoolean).toOption)
            .getOrElse(false)
        )

    override def ipAddress: String =
      request.headers.get(HeaderNames.X_FORWARDED_FOR).getOrElse(request.remoteAddress)

    override def organization =
      Organization(request.headers.get(ExtraHeaders.AUTH_ORGANIZATION_HEADER))

    override def maybeSubject =
      request.headers.get(ExtraHeaders.AUTH_SUBJECT_HEADER).map(Subject.apply)

    override def userAgent: Option[String] = request.headers.get(HeaderNames.USER_AGENT)

    override def forwardedHost: ForwardedHost =
      ForwardedHost(request.headers.get(HeaderNames.X_FORWARDED_HOST))
  }
}
