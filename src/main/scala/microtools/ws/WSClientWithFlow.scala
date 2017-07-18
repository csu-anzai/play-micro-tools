package microtools.ws

import microtools.models.{AuthRequestContext, ExtraHeaders, RequestContext, ServiceName}
import play.api.http.HeaderNames
import play.api.libs.ws.{WSClient, WSRequest}

class WSClientWithFlow(val underlying: WSClient) {
  private[this] object ForwardProto extends Enumeration {
    type Type = Value

    val HTTP  = Value("http")
    val HTTPS = Value("https")
  }

  def url(rawUrl: String)(implicit ctx: RequestContext): WSRequest = {
    val (url, forwardProto): (String, ForwardProto.Type) =
      if (rawUrl.startsWith("https:"))
        s"http:${rawUrl.drop(6)}" -> ForwardProto.HTTPS
      else rawUrl                 -> ForwardProto.HTTP

    underlying
      .url(url)
      .withHttpHeaders(ExtraHeaders.FLOW_ID_HEADER   -> ctx.flowId,
                       HeaderNames.X_FORWARDED_PROTO -> forwardProto.toString)
  }

  def urlWithAuthFromContext(rawUrl: String)(implicit ctx: AuthRequestContext): WSRequest = {
    url(rawUrl)
      .withHttpHeaders(HeaderNames.AUTHORIZATION -> s"Bearer ${ctx.token}")
  }

  def urlWithServiceAuth(rawUrl: String)(implicit serviceName: ServiceName,
                                         ctx: RequestContext): WSRequest = {
    url(rawUrl)
      .withHttpHeaders(
        ExtraHeaders.AUTH_SUBJECT_HEADER -> s"service/$serviceName"
      )
  }
}
