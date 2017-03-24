package microtools.actions

import microtools.logging.LoggingContext
import microtools.models.{CustomerSubject, Organization, ServiceName, ServiceSubject, Subject}
import play.api.mvc.{Action, AnyContent, Controller}

import scala.concurrent.{ExecutionContext, Future}

class DemoController(implicit ec: ExecutionContext) extends Controller with AuthActions {
  // This is something provided by a dao of some kind
  private def isOwnerOf(resourceId: String): ScopeRequirement.AccessCheck = {
    case (CustomerSubject(customer), _) => resourceId.endsWith(customer)
  }

  // This is something that should be centralized in a trait
  implicit val serviceName: ServiceName = ServiceName("demo-service")

  private def isCustomerService: ScopeRequirement.AccessCheckWithLogging =
    new ScopeRequirement.AccessCheckWithLogging {
      override def check(subject: Subject, organization: Organization)(
          implicit loggingContext: LoggingContext) = (subject, organization) match {
        case (ServiceSubject("customer"), _) => true
      }
    }

  // Controller specific stuff

  private def readRequirements(resourceId: String) =
    StandardScopeRequirements.checkedReadWithLogging(isCustomerService) or
      StandardScopeRequirements.checkedSelf(isOwnerOf(resourceId))

  private def updateRequirements(resourceId: String) =
    StandardScopeRequirements.checkedWriteWithLogging(isCustomerService) or
      StandardScopeRequirements.checkedSelf(isOwnerOf(resourceId))

  def getProtectedResource(id: String): Action[AnyContent] =
    (AuthAction andThen ScopedAction(readRequirements(id))).async { implicit request =>
      Future.successful(Ok("Got it"))
    }

  def updateProtectedResource(id: String): Action[AnyContent] =
    (AuthAction andThen ScopedAction(updateRequirements(id))).async { implicit request =>
      Future.successful(Accepted("Updated it"))
    }
}
