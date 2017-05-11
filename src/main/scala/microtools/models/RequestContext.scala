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
}

object RequestContext {
  def static(staticFlowId: String): RequestContext = new RequestContext {
    override def flowId: String = staticFlowId

    override lazy val contextValues: Seq[(String, String)] = Seq(
      "flow_id" -> flowId
    )

    override def enableBusinessDebug: Boolean = false

    override def ipAddress = ""

    override def userAgent = None
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
      "flow_id" -> flowId,
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

    override def ipAddress =
      request.headers.get(HeaderNames.X_FORWARDED_FOR).getOrElse(request.remoteAddress)

    override def userAgent = request.headers.get(HeaderNames.USER_AGENT)
  }
}
