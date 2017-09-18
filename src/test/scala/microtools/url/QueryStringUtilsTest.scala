package microtools.url

import org.scalatest.{MustMatchers, WordSpec}
import microtools.url.QueryStringUtils._

class QueryStringUtilsSpec extends WordSpec with MustMatchers {

  "Query string utils" should {
    "convert a map to a query string" in {
      val params = Map("fluor" -> "kalium", "Bromium" -> "!!")
      params.toQueryString must equal("fluor=kalium&Bromium=%21%21")
    }

    "convert an empty map to an empty query string" in {
      val params = Map.empty[String, String]
      params.toQueryString must equal("")
    }

    "convert a query string to a map" in {
      "fluor=kalium&Bromium=%21%21".toQueryParams.get must equal(
        Map("fluor" -> "kalium", "Bromium" -> "!!"))
    }

    "convert an empty query string to an empty map" in {
      "".toQueryParams.get must equal(Map.empty[String, String])
    }
  }

}
