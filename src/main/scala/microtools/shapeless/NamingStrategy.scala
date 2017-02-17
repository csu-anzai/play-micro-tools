package microtools.shapeless

import play.api.libs.json.{JsObject, JsPath, JsValue}

trait NamingStrategy {
  def nameFor[T](t: T)(implicit m: Manifest[T]): JsObject

  def verify[T](json: JsValue)(implicit m: Manifest[T]): Boolean
}

class ClassNameNamingStrategy(path: JsPath) extends NamingStrategy {
  val writer = path.write[String]
  val reader = path.read[String]

  override def nameFor[T](t: T)(implicit m: Manifest[T]): JsObject =
    writer.writes(m.runtimeClass.getSimpleName)

  override def verify[T](json: JsValue)(implicit m: Manifest[T]): Boolean =
    reader.reads(json).filter(_ == m.runtimeClass.getSimpleName).isSuccess
}
