package microtools.filters.cors

import org.scalatest.matchers.{MatchResult, Matcher}
import play.filters.cors.CORSConfig.Origins.Matching
import org.scalatest.{MustMatchers, WordSpec}

class GlobbingMatchingSpec extends WordSpec with MustMatchers {

  private val matching: Matching = GlobbingMatching(
    Seq(
      "https://*.21re.tools",
      "https://21re.tools",
      "https://*.21re.sxoe",
      "https://api.21re.kuci"
    )).matching()

  def matchMatching(matching: Matching) = new Matcher[String] {
    def apply(value: String): MatchResult = {
      val failureMessageSuffix =
        s"$value did not match"

      val negatedFailureMessageSuffix =
        s"$value matched"

      MatchResult(
        matching(value),
        "The " + failureMessageSuffix,
        "The " + negatedFailureMessageSuffix,
        "the " + failureMessageSuffix,
        "the " + negatedFailureMessageSuffix
      )
    }
  }

  "GlobbingMatching" should {
    "process origins" in {
      "https://geo.21re.tools" must matchMatching(matching)
      "https://21re.tools" must matchMatching(matching)
      "https://notification.21re.tools" must matchMatching(matching)
      "https://api.21re.kuci" must matchMatching(matching)

      "https://21re.sxoe" mustNot matchMatching(matching)
      "https://geo.21re.de" mustNot matchMatching(matching)
      "https://geo.21re.tools.evil" mustNot matchMatching(matching)
      "https://fkbr.21re.kuci" mustNot matchMatching(matching)
      "http://api.21re.kuci" mustNot matchMatching(matching)
    }
  }
}
