package microtools.models

import java.time.format.DateTimeFormatter
import java.time.{DateTimeException, Instant}

import play.api.data.validation.ValidationError
import play.api.libs.json._

trait Protocol {
  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] =
    new Reads[E#Value] {
      def reads(json: JsValue): JsResult[E#Value] = json match {
        case JsString(s) => {
          try {
            JsSuccess(enum.withName(s))
          } catch {
            case _: NoSuchElementException =>
              JsError(
                s"Enumeration expected of type: '${enum.getClass}', but it does not appear to contain the value: '$s'")
          }
        }
        case _ => JsError("String value expected")
      }
    }

  def enumWrites[E <: Enumeration]: Writes[E#Value] =
    new Writes[E#Value] {
      def writes(v: E#Value): JsValue = JsString(v.toString)
    }

  def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
    Format(enumReads(enum), enumWrites)
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
