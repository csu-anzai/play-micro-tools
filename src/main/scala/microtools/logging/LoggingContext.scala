package microtools.logging

/**
  * A generic logging context.
  * A logging context contains all information that should appear in every log entry. E.g.
  *   - application version
  *   - uri of the request
  *   - flow/correlation-id of the request
  *
  * Additionally a logging context enables/disables business debugging. "Business Debug" log entry may contain
  * information that you usually do not want to see in your log-files for security or legal reasons
  * (e.g. credit-card numbers, phone number, customer addresses ...), but might be very useful understanding a
  * specific problem. You may decide to activate business debug on-demand by setting a specific HTTP hreader on
  * your requests.
  */
trait LoggingContext {
  def enableBusinessDebug: Boolean

  def contextValues: Seq[(String, String)]
}

object LoggingContext {
  def static(contextValues: (String, String)*): LoggingContext =
    new LoggingContext {
      override def contextValues: Seq[(String, String)] = contextValues

      override def enableBusinessDebug: Boolean = false
    }
}
