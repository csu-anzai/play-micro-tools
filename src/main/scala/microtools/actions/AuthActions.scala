package microtools.actions

import microtools.logging.WithContextAwareLogger
import microtools.models.{AuthRequestContext, ExtraHeaders, Problems}
import play.api.mvc._
import play.mvc.Http.HeaderNames

import scala.concurrent.Future

trait AuthActions extends WithContextAwareLogger { self: Controller =>
  import AuthActions._

  def AuthAction: ActionBuilder[AuthRequest] = new ActionBuilder[AuthRequest] {
    override def invokeBlock[A](request: Request[A],
                                block: (AuthRequest[A]) => Future[Result]): Future[Result] = {
      val businessDebug = Helper.isBusinessDebug(request)
      val flowId        = Helper.getOrCreateFlowId(request)
      val ipAddress = request.headers
        .get(HeaderNames.X_FORWARDED_FOR)
        .getOrElse(request.remoteAddress)
      val userAgent = request.headers.get(HeaderNames.USER_AGENT)

      (for {
        subject <- request.headers.get(ExtraHeaders.AUTH_SUBJECT_HEADER)
        token   <- request.headers.get(ExtraHeaders.AUTH_TOKEN_HEADER)
      } yield {
        val authRequest =
          new AuthRequest(
            enableBusinessDebug = businessDebug,
            flowId = flowId,
            subject = subject,
            organization = request.headers.get(ExtraHeaders.AUTH_ORGANIZATION_HEADER),
            scopes = Map.empty,
            token = token,
            ipAddress = ipAddress,
            userAgent = userAgent,
            requestUri = request.uri,
            request = request
          )

        block(authRequest)
      }).getOrElse(Future.successful(Problems.UNAUTHORIZED.asResult))
    }
  }
}

object AuthActions {
  class AuthRequest[A](
      override val enableBusinessDebug: Boolean,
      override val flowId: String,
      override val subject: String,
      override val organization: Option[String],
      override val scopes: Map[String, Seq[String]],
      override val token: String,
      override val ipAddress: String,
      override val userAgent: Option[String],
      requestUri: String,
      request: Request[A]
  ) extends WrappedRequest[A](request)
      with AuthRequestContext {
    override def contextValues: Seq[(String, String)] = Seq(
      "flow_id"     -> flowId,
      "request_uri" -> requestUri,
      "subject"     -> subject
    )
  }
}
