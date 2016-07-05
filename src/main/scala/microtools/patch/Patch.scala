package microtools.patch

import microtools.models.Problems
import microtools.{BusinessTry, JsonFormats}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.concurrent.ExecutionContext

/**
  * RFC6902 kind of patch operation.
  */
case class Patch(op: PatchOperation.Type, path: String, value: Option[JsValue]) {
  def apply(json: JsValue)(
      implicit ec: ExecutionContext): BusinessTry[JsValue] =
    for {
      transformer <- transformation
      result      <- BusinessTry.transformJson(json, transformer)
    } yield result

  def transformation(
      implicit ec: ExecutionContext): BusinessTry[Reads[_ <: JsValue]] = {
    JsonPointer(path).flatMap { path =>
      (op, value) match {
        case (PatchOperation.ADD, Some(v)) =>
          BusinessTry.success(path.json.update(new Reads[JsValue] {
            override def reads(json: JsValue): JsResult[JsValue] = json match {
              case JsArray(elements) => JsSuccess(JsArray(elements :+ v))
              case JsNull            => JsSuccess(v)
              case existing          => JsError("error.patch.add.value.exists")
            }
          }))
        case (PatchOperation.REMOVE, None) =>
          BusinessTry.success(path.json.prune)
        case (PatchOperation.REPLACE, Some(v)) =>
          BusinessTry.success((__.read[JsObject] and path.json.put(v)).reduce)
        case _ =>
          BusinessTry.failure(
              Problems.BAD_REQUEST.withDetails(s"Invalid patch: ${this}"))
      }
    }
  }
}

object Patch extends JsonFormats {
  implicit val patchOperatioFormat = enumFormat(
      PatchOperation, normalize = _.toLowerCase)
  implicit val patchFormat = Json.format[Patch]
}
