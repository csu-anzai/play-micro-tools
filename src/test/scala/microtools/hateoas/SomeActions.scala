package microtools.hateoas

object SomeActions {
  case object GetData extends BusinessAction {
    override def rel: String = "self"
  }

  case object DeleteData extends BusinessAction {
    override def rel: String = "delete"
  }

  implicit val linkBuilder : LinkBuilder = new LinkBuilder {
    override def actionLink(action: BusinessAction): Link = action match {
      case GetData => Link(href = "/data")
      case DeleteData => Link(href = "/data", method = Some("DELETE"))
    }
  }
}
