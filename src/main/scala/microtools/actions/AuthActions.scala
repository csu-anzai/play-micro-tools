package microtools.actions

import microtools.logging.WithContextAwareLogger
import microtools.models._
import play.api.mvc._
import play.mvc.Http.HeaderNames

import scala.concurrent.{ExecutionContext, Future}

trait AuthActions extends WithContextAwareLogger {
  import AuthActions._

  def AuthAction(implicit ec: ExecutionContext): ActionBuilder[AuthRequest] =
    new ActionBuilder[AuthRequest] {
      override def invokeBlock[A](
          request: Request[A],
          block: (AuthRequest[A]) => Future[Result]
      ): Future[Result] = {
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
              subject = Subject(subject),
              organization =
                Organization(request.headers.get(ExtraHeaders.AUTH_ORGANIZATION_HEADER)),
              scopes = ScopesByService.fromHeaders(request.headers),
              token = Token(token),
              ipAddress = ipAddress,
              userAgent = userAgent,
              requestUri = request.uri,
              request = request
            )

          block(authRequest).map(_.withHeaders(ExtraHeaders.FLOW_ID_HEADER -> flowId))
        }).getOrElse {
          Future.successful(Problems.UNAUTHORIZED.asResult(RequestContext.forRequest(request)))
        }
      }
    }
}

object AuthActions {
  type AuthRequest[A] = GenericAuthRequest[A, Subject, Organization]

  type CustomerAuthRequest[A] = GenericAuthRequest[A, CustomerSubject, GenericOrganization]

  type ServiceAuthRequest[A] = GenericAuthRequest[A, ServiceSubject, NoOrganization.type]

  type AdminAuthRequest[A] = GenericAuthRequest[A, AdminSubject, NoOrganization.type]

  def forRequest[A, S <: Subject, O <: Organization](
      request: AuthRequest[A],
      subject: S,
      organization: O): GenericAuthRequest[A, S, O] = {
    new GenericAuthRequest[A, S, O](
      enableBusinessDebug = request.enableBusinessDebug,
      flowId = request.flowId,
      subject = subject,
      organization = organization,
      scopes = request.scopes,
      token = request.token,
      ipAddress = request.ipAddress,
      userAgent = request.userAgent,
      requestUri = request.uri,
      request = request
    )
  }

  class GenericAuthRequest[A, +Sub <: Subject, +Org <: Organization](
      override val enableBusinessDebug: Boolean,
      override val flowId: String,
      override val subject: Sub,
      override val organization: Org,
      override val scopes: ScopesByService,
      override val token: Token,
      override val ipAddress: String,
      override val userAgent: Option[String],
      requestUri: String,
      request: Request[A]
  ) extends WrappedRequest[A](request)
      with GenericAuthRequestContext[Sub, Org] {
    override def contextValues: Seq[(String, String)] = Seq(
      "flow_id"      -> flowId,
      "request_uri"  -> requestUri,
      "subject"      -> subject.toString,
      "organization" -> organization.toString
    )
  }
}
