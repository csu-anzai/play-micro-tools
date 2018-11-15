package microtools.actions

import java.nio.charset.StandardCharsets
import microtools.logging.{LoggingContext, WithContextAwareLogger}
import microtools.models._
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import play.mvc.Http.HeaderNames

import scala.concurrent.{ExecutionContext, Future}

trait AuthActions extends WithContextAwareLogger { self: AbstractController =>

  import AuthActions._

  def AuthAction(implicit ec: ExecutionContext): ActionBuilder[AuthRequest, AnyContent] =
    AuthAction(self.controllerComponents.parsers.default)

  def AuthAction[B](bodyParser: BodyParser[B])(
      implicit ec: ExecutionContext): ActionBuilder[AuthRequest, B] =
    new ActionBuilder[AuthRequest, B] {
      override def parser: BodyParser[B] = bodyParser

      override protected def executionContext: ExecutionContext = ec

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
        val language = request.cookies
          .get(CookieNames.LANGUAGE)
          .map(_.value.toLowerCase)
          .getOrElse("de")

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
              request = request,
              language = language
            )

          block(authRequest).map(_.withHeaders(ExtraHeaders.FLOW_ID_HEADER -> flowId))
        }).getOrElse {
          Future.successful(Problems.UNAUTHORIZED.asResult(RequestContext.forRequest(request)))
        }
      }
    }

  def BasicAuthAction(credentials: BasicAuthCredentials)(
      implicit ec: ExecutionContext): ActionBuilder[BasicAuthRequest, AnyContent] =
    BasicAuthAction(credentials, self.controllerComponents.parsers.default)

  def BasicAuthAction[B](credentials: BasicAuthCredentials, bodyParser: BodyParser[B])(
      implicit ec: ExecutionContext): ActionBuilder[BasicAuthRequest, B] =
    new ActionBuilder[BasicAuthRequest, B] {
      override def parser: BodyParser[B] = bodyParser

      override protected def executionContext: ExecutionContext = ec

      override def invokeBlock[A](request: Request[A],
                                  block: BasicAuthRequest[A] => Future[Result]): Future[Result] = {
        request.headers.get("Authorization") match {
          case Some(authHeader) =>
            if (userIsAuthenticated(request, authHeader, credentials))
              block(new BasicAuthRequest(request))
            else Future.successful(UNAUTHORIZED_BASIC_AUTH)
          case None => Future.successful(UNAUTHORIZED_BASIC_AUTH)
        }
      }
    }

  private def userIsAuthenticated[A](request: Request[A],
                                     authHeader: String,
                                     credentials: BasicAuthCredentials): Boolean = {
    authHeader.split("""\s""") match {
      case Array("Basic", userAndPass) => userAndPass == credentials.asBase64String
      case Array("Bearer", _) =>
        implicit val requestContext: RequestContext = RequestContext.forRequest(request)

        log.warn("Received a Bearer token, please inspect your proxy setup")

        false
      case _ => false
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
      language = request.language,
      requestUri = request.uri,
      request = request
    )
  }

  class BasicAuthRequest[A](request: Request[A])
      extends WrappedRequest[A](request)
      with LoggingContext {
    val businessDebug = Helper.isBusinessDebug(request)
    val flowId        = Helper.getOrCreateFlowId(request)
    val ipAddress = request.headers
      .get(HeaderNames.X_FORWARDED_FOR)
      .getOrElse(request.remoteAddress)
    val userAgent = request.headers.get(HeaderNames.USER_AGENT)
    val language = request.cookies
      .get(CookieNames.LANGUAGE)
      .map(_.value.toLowerCase)
      .getOrElse("de")

    override def contextValues: Seq[(String, String)] =
      Seq("flow_id"     -> flowId,
          "request_uri" -> request.uri,
          "ip"          -> ipAddress,
          "userAgent"   -> userAgent.getOrElse("?"),
          "language"    -> language)
    override def enableBusinessDebug: Boolean = businessDebug
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
      override val language: String,
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

  val UNAUTHORIZED_BASIC_AUTH: Result =
    Unauthorized.withHeaders(
      "WWW-Authenticate" -> s"""Basic realm="Secured", charset="${StandardCharsets.UTF_8
        .name()}"""")
}
