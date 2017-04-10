package microtools

import java.time.{DateTimeException, Instant}
import java.time.format.DateTimeFormatter

import play.api.data.validation.ValidationError
import play.api.libs.json._

/**
  * Missing or alternative json formats.
  */
trait JsonFormats {
  case class AnyValFormat[S, T <: AnyVal](anyValApply: S => T)(anyValUnapply: T => Option[S])(
      implicit format: Format[S])
      extends Format[T] {
    def reads(json: JsValue): JsResult[T] = json.validate[S].map(anyValApply)
    def writes(value: T): JsValue         = Json.toJson(anyValUnapply(value))
  }

  def enumReads[E <: Enumeration](enum: E,
                                  default: Option[E#Value] = None,
                                  normalize: String => String = identity): Reads[E#Value] =
    new Reads[E#Value] {
      def reads(json: JsValue): JsResult[E#Value] = json match {
        case JsString(s) => {
          try {
            JsSuccess(enum.withName(normalize(s)))
          } catch {
            case _: NoSuchElementException =>
              default
                .map(JsSuccess(_))
                .getOrElse(
                  JsError(Seq(JsPath() ->
                    Seq(ValidationError("error.invalid.enum.value")))))
          }
        }
        case _ =>
          JsError(
            Seq(
              JsPath() ->
                Seq(ValidationError("error.expected.string"))))
      }
    }

  def enumWrites[E <: Enumeration]: Writes[E#Value] =
    new Writes[E#Value] {
      def writes(v: E#Value): JsValue = JsString(v.toString)
    }

  def enumFormat[E <: Enumeration](enum: E,
                                   default: Option[E#Value] = None,
                                   normalize: String => String = identity): Format[E#Value] = {
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
                  Seq(ValidationError("error.expected.date.isoformat"))))
        }
      case _ =>
        JsError(
          Seq(
            JsPath() ->
              Seq(ValidationError("error.expected.date"))))
    }
  }
}
