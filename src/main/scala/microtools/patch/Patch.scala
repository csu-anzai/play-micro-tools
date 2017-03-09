package microtools.patch

import microtools.patch.JsonPointer._
import microtools.{BusinessTry, JsonFormats}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
  * RFC6902 kind of patch operation.
  */
sealed trait Patch {
  def path: JsPath
  def apply(json: JsValue): BusinessTry[JsValue] =
    BusinessTry.transformJson(json, transformation)

  def transformation: Reads[_ <: JsValue]
}

case class Remove(path: JsPath) extends Patch {
  override def transformation: Reads[JsObject] =
    path.json.prune
}
case class Add(path: JsPath, value: JsValue) extends Patch {
  override def transformation: Reads[_ <: JsValue] =
    path.json.update(Reads {
      case JsArray(elements) => JsSuccess(JsArray(elements :+ value))
      case JsNull            => JsSuccess(value)
      case existing          => JsError("error.patch.add.value.exists")
    })
}
case class Replace(path: JsPath, value: JsValue) extends Patch {
  override def transformation: Reads[_ <: JsValue] =
    (__.read[JsObject] and path.json.put(value)).reduce
}

object Patch extends JsonFormats {
  private def stringToJsPath(path: String): JsPath =
    JsonPointer.jsPathFormat.reads(JsString(path)).get

  @deprecated(
    "Use microtools.patch Add, Remove etc. classes or the add, replace, etc. methods here instead",
    "0.1-40")
  def apply(op: PatchOperation.Type, path: String, value: Option[JsValue]): Patch = {
    val jsPath = stringToJsPath(path)
    op match {
      case PatchOperation.ADD =>
        Add(jsPath, value.getOrElse(sys.error("value is required for add")))
      case PatchOperation.REPLACE =>
        Replace(jsPath, value.getOrElse(sys.error("value is required for replace")))
      case PatchOperation.REMOVE =>
        Remove(jsPath)
    }
  }

  def add(path: String, value: JsValue): Patch     = Add(stringToJsPath(path), value)
  def replace(path: String, value: JsValue): Patch = Replace(stringToJsPath(path), value)
  def remove(path: String): Patch                  = Remove(stringToJsPath(path))

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
