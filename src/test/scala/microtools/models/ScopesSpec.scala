package microtools.models

import org.scalatestplus.play.PlaySpec
import play.api.mvc.Headers

class ScopesSpec extends PlaySpec {
  "Scopes" should {
    "support contains" in {
      val scopes = Scopes("R", "W")

      scopes.contains("R") mustBe true
      scopes.contains("W") mustBe true
      scopes.contains("something") mustBe false
    }

    "extract request headers" in {
      val headers = Headers(
        "X-Auth-Scopes-Customer" -> "R",
        "X-Auth-Scopes-customer" -> "W",
        "x-auth-scopes-offer"    -> "R",
        "X-AUTH-SCOPES-object"   -> "*"
      )

      val scopesByService = ScopesByService.fromHeaders(headers)

      scopesByService.forService(ServiceName("customer")) mustBe Scopes("R", "W")
      scopesByService.forService(ServiceName("offer")) mustBe Scopes("R")
      scopesByService.forService(ServiceName("object")) mustBe Scopes("*")
    }
  }
}
