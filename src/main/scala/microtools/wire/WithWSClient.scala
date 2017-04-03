package microtools.wire

import microtools.ws.WSClientWithFlow

trait WithWSClient {
  def wsClientWithFlow: WSClientWithFlow
}
