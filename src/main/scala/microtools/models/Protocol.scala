package microtools.models

import java.time.format.DateTimeFormatter
import java.time.{DateTimeException, Instant}

import play.api.libs.json._

trait Protocol {
  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] =
    implicitly[Reads[String]].flatMap { s =>
      Reads[E#Value] { _ =>
        try {
          JsSuccess(enum.withName(s))
        } catch {
          case _: NoSuchElementException =>
            JsError(
              s"Enumeration expected of type: '${enum.getClass}', but it does not appear to contain the value: '$s'"
            )
        }
      }
    }

  def enumWrites[E <: Enumeration]: Writes[E#Value] =
    Writes[E#Value] { v: E#Value =>
      JsString(v.toString)
    }

  def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
    Format(enumReads(enum), enumWrites)
  }

  implicit def instantWrites: Writes[Instant] = Writes[Instant] { o =>
    JsNumber(o.getEpochSecond)
  }

  implicit def instantReads: Reads[Instant] = Reads[Instant] {
    case JsNumber(time) => JsSuccess(Instant.ofEpochSecond(time.toLong))
    case JsString(str) =>
      try {
        JsSuccess(Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(str)))
      } catch {
        case _: DateTimeException =>
          JsError(
            Seq(
              JsPath() ->
                Seq(JsonValidationError("error.expected.date.isoformat"))
            )
          )
      }
    case _ =>
      JsError(
        Seq(
          JsPath() ->
            Seq(JsonValidationError("error.expected.date"))
        )
      )
  }
}
