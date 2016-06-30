package microtools.patch

import microtools.{BusinessTry, JsonFormats}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext

/**
  * RFC6902 kind of patch operation.
  */
case class Patch(op: PatchOperation.Type, path: String, value: JsValue) {
  def apply(json: JsValue)(
      implicit ec: ExecutionContext): BusinessTry[JsValue] =
    for {
      transformer <- transformation
      result      <- BusinessTry.transformJson(json, transformer)
    } yield result

  def transformation(
      implicit ec: ExecutionContext): BusinessTry[Reads[_ <: JsValue]] = {
    JsonPointer(path).map { path =>
      op match {
        case PatchOperation.REPLACE =>
          (__.json.pick and path.json.put(value)).reduce
      }
    }
  }
}

object Patch extends JsonFormats {
  implicit val patchOperatioFormat = enumFormat(PatchOperation, _.toLowerCase)
  implicit val patchFormat         = Json.format[Patch]
}
