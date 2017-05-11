package microtools.actions

import java.util.UUID

import microtools.models.ExtraHeaders
import play.api.mvc.RequestHeader

import scala.util.Try

object Helper {
  def isBusinessDebug(rh: RequestHeader): Boolean =
    rh.cookies
      .get(ExtraHeaders.DEBUG_HEADER)
      .flatMap(c => Try(c.value.toBoolean).toOption)
      .getOrElse(
        rh.headers
          .get(ExtraHeaders.DEBUG_HEADER)
          .flatMap(s => Try(s.toBoolean).toOption)
          .getOrElse(false)
      )

  def getOrCreateFlowId(rh: RequestHeader): String =
    rh.cookies
      .get(ExtraHeaders.FLOW_ID_HEADER)
      .map(_.value)
      .getOrElse(
        rh.headers
          .get(ExtraHeaders.FLOW_ID_HEADER)
          .getOrElse(generateFlowId())
      )

  def generateFlowId(): String = UUID.randomUUID().toString
}
