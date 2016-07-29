package microtools.models

import java.util.UUID

import microtools.logging.LoggingContext
import play.api.mvc.RequestHeader

import scala.util.Try

trait RequestContext extends LoggingContext {
  def flowId: String
}

object RequestContext {
  def static(staticFlowId: String) = new RequestContext {
    override def flowId: String = staticFlowId

    override def contextValues: Seq[(String, String)] = Seq.empty

    override def enableBusinessDebug: Boolean = false
  }

  def forRequest(request: RequestHeader) = new RequestContext {
    override def flowId: String = request.cookies
      .get(ExtraHeaders.FLOW_ID_HEADER)
      .map(_.value)
      .getOrElse(request.headers
        .get(ExtraHeaders.FLOW_ID_HEADER)
        .getOrElse(UUID.randomUUID().toString))

    override def contextValues: Seq[(String, String)] = Seq(
      "flow_id"     -> flowId,
      "request_uri" -> request.uri
    )

    override def enableBusinessDebug: Boolean = request.cookies
      .get(ExtraHeaders.DEBUG_HEADER)
      .flatMap(c => Try(c.value.toBoolean).toOption)
      .getOrElse(request.headers
        .get(ExtraHeaders.DEBUG_HEADER)
        .flatMap(s => Try(s.toBoolean).toOption)
        .getOrElse(false))
  }
}
