package microtools

import java.time.{DateTimeException, Instant}
import java.time.format.DateTimeFormatter

import play.api.data.validation.ValidationError
import play.api.libs.json._
import _root_.shapeless.{Generic, ::, HNil, HList}

/**
  * Missing or alternative json formats.
  */
trait JsonFormats {
  val AnyValFormat = JsonFormats.WrapperFormat
  type AnyValFormat[T, S <: AnyVal] = JsonFormats.WrapperFormat[T, S]

  implicit def formatAnyVal[T <: AnyVal, L <: HList, S](implicit gen: Generic.Aux[T, L],
                                                        ev1: (S :: HNil) =:= L,
                                                        ev2: L =:= (S :: HNil),
                                                        format: Format[S]): Format[T] =
    AnyValFormat[S, T](s => gen.from(ev1(s :: HNil)))(t => Some(gen.to(t).head))

  def enumReads[E <: Enumeration](
      enum: E,
      default: Option[E#Value] = None,
      normalize: String => String = identity
  ): Reads[E#Value] =
    implicitly[Reads[String]].flatMap { s =>
      Reads { _ =>
        try {
          JsSuccess(enum.withName(normalize(s)))
        } catch {
          case _: NoSuchElementException =>
            default
              .map(JsSuccess(_))
              .getOrElse(JsError(ValidationError("error.invalid.enum.value")))
        }
      }
    }

  def enumWrites[E <: Enumeration]: Writes[E#Value] =
    new Writes[E#Value] {
      def writes(v: E#Value): JsValue = JsString(v.toString)
    }

  def enumFormat[E <: Enumeration](
      enum: E,
      default: Option[E#Value] = None,
      normalize: String => String = identity
  ): Format[E#Value] = {
    Format(enumReads(enum, default, normalize), enumWrites)
  }

  implicit def instantWrites: Writes[Instant] = new Writes[Instant] {
    override def writes(o: Instant): JsValue = JsNumber(o.getEpochSecond)
  }

  implicit def instantReads: Reads[Instant] = new Reads[Instant] {
    override def reads(json: JsValue): JsResult[Instant] = json match {
      case JsNumber(time) => JsSuccess(Instant.ofEpochSecond(time.toLong))
      case JsString(str) =>
        try {
          JsSuccess(Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(str)))
        } catch {
          case _: DateTimeException =>
            JsError(
              Seq(
                JsPath() ->
                  Seq(ValidationError("error.expected.date.isoformat"))
              )
            )
        }
      case _ =>
        JsError(
          Seq(
            JsPath() ->
              Seq(ValidationError("error.expected.date"))
          )
        )
    }
  }
}

object JsonFormats {
  case class WrapperFormat[S, T](wrap: S => T)(unwrap: T => Option[S])(
      implicit format: Format[S]
  ) extends Format[T] {
    def reads(json: JsValue): JsResult[T] = json.validate[S].map(wrap)
    def writes(value: T): JsValue         = Json.toJson(unwrap(value))
  }

  def wrapperFormat[T, S: Format](implicit gen: Generic.Aux[T, S :: HNil]): Format[T] =
    new WrapperFormat[S, T](s => gen.from(s :: HNil))(t => Some(gen.to(t).head))
}
