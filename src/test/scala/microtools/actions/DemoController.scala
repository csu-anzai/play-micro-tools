package microtools.actions

import microtools.BusinessTry
import microtools.logging.LoggingContext
import microtools.models._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class DemoController(controllerComponents: ControllerComponents)(implicit ec: ExecutionContext)
    extends AbstractController(controllerComponents)
    with AuthActions {
  // This is something provided by a dao of some kind
  private def isOwnerOf(resourceId: String): ScopeRequirement.AccessCheck = {
    case (CustomerSubject(customer), _) => BusinessTry.success(resourceId.endsWith(customer))
  }

  // This is something that should be centralized in a trait
  implicit val serviceName: ServiceName = ServiceName("demo-service")

  private def isCustomerService: ScopeRequirement.AccessCheckWithLogging =
    new ScopeRequirement.AccessCheckWithLogging {
      override def check(subject: Subject, organization: Organization)(
          implicit loggingContext: LoggingContext,
          ec: ExecutionContext): BusinessTry[Boolean] = {
        val contextValues = loggingContext.contextValues.toMap
        log.info(s"Flow Id: ${contextValues.getOrElse("flowId", "unknown")}")
        (subject, organization) match {
          case (ServiceSubject("customer"), _) =>
            log.info(
              s"Customer Service check was successful for subject $subject and organization $organization")
            BusinessTry.success(true)
          case _ =>
            log.info(
              s"Customer Service check was unsuccessful for subject $subject and organization $organization")
            BusinessTry.success(false)
        }
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
