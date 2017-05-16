package microtools.filters

import javax.inject.{Inject, Named, Singleton}

import akka.stream.Materializer
import microtools.logging.WithContextAwareLogger
import microtools.models.RequestContext
import play.api.http.HeaderNames
import play.api.mvc.{Filter, RequestHeader, Result, Results}

import scala.concurrent.Future

@Singleton
class RedirectHttpsFilter @Inject()(@Named("base.url") baseUrl: String)(
    implicit override val mat: Materializer
) extends Filter
    with Results
    with WithContextAwareLogger {

  override def apply(nextFilter: RequestHeader => Future[Result])(
      rh: RequestHeader
  ): Future[Result] = {

    implicit val requestContext = RequestContext.forRequest(rh)

    if (rh.headers.get(HeaderNames.X_FORWARDED_PROTO).contains("http")) {
      log.warn(
        s"Forwarded protocol contains http. Service responds with SeeOther: $baseUrl${rh.path}?${rh.rawQueryString}"
      )
      Future.successful(SeeOther(baseUrl + rh.path + "?" + rh.rawQueryString))
    } else {
      nextFilter(rh)
    }
  }
}
