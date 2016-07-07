package microtools.models

import microtools.logging.LoggingContext

trait RequestContext extends LoggingContext {
  def flowId: String
}

object RequestContext {
  def static(staticFlowId: String) = new RequestContext {
    override def flowId: String = staticFlowId

    override def contextValues: Seq[(String, String)] = Seq.empty

    override def enableBusinessDebug: Boolean = false
  }
}
