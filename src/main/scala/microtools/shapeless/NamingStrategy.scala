package microtools.shapeless

import play.api.libs.json.{ JsObject, JsPath, JsValue }

trait NamingStrategy[T] {
  def nameFor(t: T): JsObject

  def verify(json: JsValue): Boolean
}

class ClassNameNamingStrategy(path: JsPath) {
  val classNameWriter = path.write[String]
  val classNameReader = path.read[String]

  implicit def namingFor[T](implicit m: Manifest[T]): NamingStrategy[T] = new NamingStrategy[T] {
    override def nameFor(t: T): JsObject = classNameWriter.writes(m.runtimeClass.getSimpleName)

    override def verify(json: JsValue): Boolean =
      classNameReader.reads(json).filter(_ == m.runtimeClass.getSimpleName).isSuccess
  }
}
