package microtools.patch

import microtools.JsonFormats
import play.api.libs.json.{JsValue, Json}

/**
  * RFC6902 kind of patch operation.
  */
case class Patch(op: PatchOperation.Type, path: String, value: JsValue)

object Patch extends JsonFormats {
  implicit val patchOperatioFormat = enumFormat(PatchOperation, _.toLowerCase)
  implicit val patchFormat = Json.format[Patch]
}
