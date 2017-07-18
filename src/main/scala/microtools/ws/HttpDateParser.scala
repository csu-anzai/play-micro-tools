package microtools.ws

import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.{Date, Locale}

import scala.util.Try

object HttpDateParser {
  private val PROPER_FORMAT_RFC822 = DateTimeFormatter.RFC_1123_DATE_TIME
  private val OBSOLETE_FORMAT1_RFC850 =
    DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss z", Locale.ENGLISH)
  private val OBSOLETE_FORMAT2_ANSIC =
    DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy", Locale.ENGLISH)

  private def parseZonedDateTimeSilent(text: String, formatter: DateTimeFormatter) =
    Try(Date.from(ZonedDateTime.parse(text, formatter).toInstant))

  private def parseDateTimeSilent(text: String, formatter: DateTimeFormatter) =
    Try(Date.from(LocalDateTime.parse(text, formatter).toInstant(ZoneOffset.UTC)))

  def parse(text: String): Option[Date] = {
    parseZonedDateTimeSilent(text, PROPER_FORMAT_RFC822)
      .recoverWith {
        case _ => parseZonedDateTimeSilent(text, OBSOLETE_FORMAT1_RFC850)
      }
      .recoverWith {
        case _ => parseDateTimeSilent(text, OBSOLETE_FORMAT2_ANSIC)
      }
      .toOption
  }
}
