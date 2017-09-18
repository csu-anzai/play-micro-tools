package microtools.url

import java.net.{URLEncoder, URLDecoder}
import java.nio.charset.StandardCharsets
import scala.util.Try
object QueryStringUtils {

  implicit class mapToQueryString(queryParams: Map[String, String]) {
    def toQueryString: String = {
      queryParams
        .map {
          case (key, value) =>
            val encodedKey   = URLEncoder.encode(key, StandardCharsets.UTF_8.toString)
            val encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.toString)
            s"$encodedKey=$encodedValue"
        }
        .mkString("&")
    }
  }

  implicit class queryStringToMap(queryString: String) {
    def toQueryParams: Try[Map[String, String]] = Try {
      queryString
        .split("&")
        .toList
        .filterNot(_.isEmpty)
        .map { param =>
          val keyValue = param.split("=")
          (URLDecoder.decode(keyValue(0), StandardCharsets.UTF_8.toString),
           URLDecoder.decode(keyValue(1), StandardCharsets.UTF_8.toString))
        }
        .toMap
    }
  }
}
