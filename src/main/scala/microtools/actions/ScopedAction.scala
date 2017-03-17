package microtools.actions

case class ServiceName(name: String) extends AnyVal

case class ScopedAction()(implicit serviceName: ServiceName) {
      extends ActionBuilder[Request] {
    override def invokeBlock[A](request: Request[A],
                                block: (Request[A]) => Future[Result]): Future[Result] = {
          block(request)
    }
  }
}
