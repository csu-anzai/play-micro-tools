package microtools.ws

import microtools.models.{AuthRequestContext, ExtraHeaders, RequestContext, ServiceName}
import play.api.http.HeaderNames
import play.api.libs.ws.{WSClient, WSRequest}

object ForwardProto extends Enumeration {
  type Type = Value

  val HTTP  = Value("http")
  val HTTPS = Value("https")
}

class WSClientWithFlow(val underlying: WSClient) {

  def url(rawUrl: String)(implicit forwardProto: ForwardProto.Type,
                          ctx: RequestContext): WSRequest = {
    val url: String =
      if (rawUrl.startsWith("https:"))
        s"http:${rawUrl.drop(6)}"
      else rawUrl

    val headers = Seq(HeaderNames.X_FORWARDED_PROTO -> forwardProto.toString())
    underlying
      .url(url)
      .withHeaders(ExtraHeaders.FLOW_ID_HEADER -> ctx.flowId)
  }

  def urlWithAuthFromContext(rawUrl: String)(implicit forwardProto: ForwardProto.Type,
                                             ctx: AuthRequestContext): WSRequest = {
    url(rawUrl)
      .withHeaders(HeaderNames.AUTHORIZATION -> s"Bearer ${ctx.token}")
  }

  def urlWithServiceAuth(rawUrl: String)(implicit forwardProto: ForwardProto.Type,
                                         serviceName: ServiceName,
                                         ctx: RequestContext): WSRequest = {
    url(rawUrl)
      .withHeaders(
        ExtraHeaders.AUTH_SUBJECT_HEADER -> s"service/$serviceName"
      )
  }
}
