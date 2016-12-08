package microtools.wire

import play.api.libs.ws.WSClient

trait WithWSClient {
  def wsClient: WSClient
}
