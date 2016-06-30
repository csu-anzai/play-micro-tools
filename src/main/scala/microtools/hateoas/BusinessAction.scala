package microtools.hateoas

/**
  * Any kind of business action you may perform with a resource.
  */
trait BusinessAction {
  /**
    * The relation of the action as it should appear in the _links.
    */
  def rel: String
}
