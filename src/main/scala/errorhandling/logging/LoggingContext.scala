package errorhandling.logging

trait LoggingContext {
  def enableBusinessDebug: Boolean

  def contextValues: Seq[(String, String)]
}
