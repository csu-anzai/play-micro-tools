package errorhandling.hateoas

/**
  * Convert a business action to a link.
  * This this is supposed to be the glue between the business logic and the corresponding routes.
  */
trait LinkBuilder {
  def actionLink(action: BusinessAction):Link
}
