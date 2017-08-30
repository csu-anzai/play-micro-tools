package microtools.patch

import microtools.models.Problems
import microtools.patch.JsonPointer._
import microtools.{BusinessTry, JsonFormats}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class PatchWhitelist(allowed: Seq[JsPath]) extends AnyVal

/**
  * RFC6902 kind of patch operation.
  */
sealed trait Patch {
  def path: JsPath
  @deprecated(
    "UNCHECKED PATCH APPLICATIONS ARE HUGE SECURITY LIABILITY, please use `patch.apply(JsValue, PatchWhitelist)` instead to specify which paths are ok to patch (hint: not all, usually not the id or the owner etc..)",
    "20170531v0114"
  )
  def apply(json: JsValue): BusinessTry[JsValue] = {
    BusinessTry.transformJson(json, transformation)
  }
  def apply(json: JsValue, ev: PatchWhitelist): BusinessTry[JsValue] = {
    if (ev.allowed contains path)
      BusinessTry.transformJson(json, transformation)
    else
      BusinessTry.failure(Problems.FORBIDDEN.withDetails(s"patch operation not allowed on $path"))
  }

  def transformation: Reads[_ <: JsValue]
}

case class Remove(path: JsPath) extends Patch {
  override def transformation: Reads[JsObject] =
    path.json.prune
}
case class Add(path: JsPath, value: JsValue) extends Patch {
  override def transformation: Reads[_ <: JsValue] = {
    new Reads[JsValue] {
      override def reads(json: JsValue): JsResult[JsValue] = {
        if (path(json).isEmpty) {
          json.validate(__.json.update(path.json.put(value)))
        } else {
          json.validate(path.json.update(Reads {
            case arr: JsArray => JsSuccess(arr :+ value)
            case JsNull       => JsSuccess(value)
            case _            => JsError("error.patch.add.value.exists")
          }))
        }
      }
    }
  }
}
case class Replace(path: JsPath, value: JsValue) extends Patch {
  override def transformation: Reads[_ <: JsValue] =
    (path.json.prune and path.json.put(value)).reduce
}

object Patch extends JsonFormats {
  val patchRead: Reads[Patch] =
    (__ \ "op").read[String].flatMap {
      case "remove"    => path.map(Remove)
      case "add"       => (path and value)(Add)
      case "replace"   => (path and value)(Replace)
      case unsupported => Reads(_ => JsError(s"Unsupported patch operation: $unsupported"))
    }

  private val patchWrite: OWrites[Patch] = OWrites {
    case Remove(path)         => Json.obj("op" -> "remove", "path"  -> path)
    case Add(path, value)     => Json.obj("op" -> "add", "path"     -> path, "value" -> value)
    case Replace(path, value) => Json.obj("op" -> "replace", "path" -> path, "value" -> value)
  }

  private lazy val path = __.\("path").read[JsPath]

  private lazy val value = __.\("value").read[JsValue]

  implicit val patchFormat: OFormat[Patch] = OFormat(patchRead, patchWrite)

}
