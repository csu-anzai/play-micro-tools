package microtools.ws

import microtools.models.{ AuthRequestContext, ExtraHeaders, RequestContext }
import play.api.http.HeaderNames
import play.api.libs.ws.{ WSClient, WSRequest }

class WSClientWithFlow(val underlying: WSClient) {

  def url(rawUrl: String)(implicit ctx: RequestContext): WSRequest = {
    underlying
      .url(rawUrl)
      .withHeaders(ExtraHeaders.FLOW_ID_HEADER -> ctx.flowId)
  }

  def urlWithAuthFromContext(rawUrl: String)(implicit ctx: AuthRequestContext): WSRequest = {
    url(rawUrl)
      .withHeaders(HeaderNames.AUTHORIZATION -> s"Bearer ${ctx.token}")
  }
}
