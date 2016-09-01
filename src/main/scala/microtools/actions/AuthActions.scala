package microtools.actions

import microtools.logging.WithContextAwareLogger
import microtools.models.{AuthRequestContext, ExtraHeaders, Problems}
import play.api.mvc._

import scala.concurrent.Future

trait AuthActions extends WithContextAwareLogger { self: Controller =>
  import AuthActions._

  def AuthAction = new ActionBuilder[AuthRequest] {
    override def invokeBlock[A](
        request: Request[A],
        block: (AuthRequest[A]) => Future[Result]): Future[Result] = {
      val businessDebug = Helper.isBusinessDebug(request)
      val flowId        = Helper.getOrCreateFlowId(request)

      (for {
        subject <- request.headers.get(ExtraHeaders.AUTH_SUBJECT_HEADER)
        token   <- request.headers.get(ExtraHeaders.AUTH_TOKEN_HEADER)
      } yield {
        val authRequest =
          new AuthRequest(businessDebug,
                          flowId,
                          subject,
                          Map.empty,
                          token,
                          request.uri,
                          request)

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
      override val scopes: Map[String, Seq[String]],
      override val token: String,
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
