package microtools.logging

import microtools.models.Problem
import microtools.{BusinessFailure, BusinessSuccess, DecidedBusinessTry}
import org.slf4j.{Logger, MDC}

import scala.util.{Failure, Success, Try}

class ContextAwareLogger(logger: Logger) {
  def name: String = logger.getName

  def withLoggingContext[T](extraValues: Seq[(String, String)] = Seq.empty)(block: => T)(
      implicit loggingContext: LoggingContext
  ): T = {
    val values = loggingContext.contextValues ++ extraValues
    try {
      values.foreach(kv => MDC.put(kv._1, kv._2))

      block
    } finally {
      values.foreach(kv => MDC.remove(kv._1))
    }
  }

  /**
    * `true` if the logger instance is enabled for the `TRACE` level.
    */
  def isTraceEnabled: Boolean = logger.isTraceEnabled

  /**
    * `true` if the logger instance is enabled for the `DEBUG` level.
    */
  def isDebugEnabled(implicit loggingContext: LoggingContext): Boolean =
    loggingContext.enableBusinessDebug || logger.isDebugEnabled

  /**
    * `true` if business debugging is enabled.
    *
    * Business will write business relevant data to the log, that must not be logged in
    * regular operation.
    */
  def isBusinessDebugEnabled(implicit loggingContext: LoggingContext): Boolean =
    loggingContext.enableBusinessDebug

  /**
    * `true` if the logger instance is enabled for the `INFO` level.
    */
  def isInfoEnabled(implicit loggingContext: LoggingContext): Boolean =
    loggingContext.enableBusinessDebug || logger.isInfoEnabled

  /**
    * `true` if the logger instance is enabled for the `WARN` level.
    */
  def isWarnEnabled(implicit loggingContext: LoggingContext): Boolean =
    loggingContext.enableBusinessDebug || logger.isWarnEnabled

  /**
    * `true` if the logger instance is enabled for the `ERROR` level.
    */
  def isErrorEnabled(implicit loggingContext: LoggingContext): Boolean =
    loggingContext.enableBusinessDebug || logger.isErrorEnabled

  /**
    * Logs a message with the `TRACE` level.
    *
    * @param message the message to log
    */
  def trace(message: => String)(implicit loggingContext: LoggingContext): Unit =
    withLoggingContext() {
      if (logger.isTraceEnabled) logger.trace(message)
    }

  /**
    * Logs a message with the `TRACE` level.
    *
    * @param message the message to log
    * @param error   the associated exception
    */
  def trace(message: => String, error: => Throwable)(
      implicit loggingContext: LoggingContext
  ): Unit = withLoggingContext() {
    if (logger.isTraceEnabled) logger.trace(message, error)
  }

  /**
    * Logs a message with the `DEBUG` level.
    *
    * @param message the message to log
    */
  def debug(message: => String)(implicit loggingContext: LoggingContext): Unit =
    withLoggingContext() {
      if (logger.isDebugEnabled) logger.debug(message)
    }

  /**
    * Logs a message with the `DEBUG` level.
    *
    * @param message the message to log
    * @param error   the associated exception
    */
  def debug(message: => String, error: => Throwable)(
      implicit loggingContext: LoggingContext
  ): Unit = withLoggingContext() {
    if (logger.isDebugEnabled) logger.debug(message, error)
  }

  /**
    * Logs a message with the `DEBUG` level and context map.
    *
    * @param message the message to log
    */
  def debug(message: => String, extraValues: (String, String)*)(
      implicit loggingContext: LoggingContext
  ): Unit =
    withLoggingContext(extraValues) {
      if (logger.isDebugEnabled) logger.debug(message)
    }

  /**
    * Logs a business debug message.
    *
    * @param message the message to log
    */
  def businessDebug(message: => String)(implicit loggingContext: LoggingContext): Unit =
    withLoggingContext() {
      if (isBusinessDebugEnabled) logger.info(message)
    }

  /**
    * Logs a message with the `INFO` level.
    *
    * @param message the message to log
    */
  def info(message: => String, extraValues: (String, String)*)(
      implicit loggingContext: LoggingContext
  ): Unit =
    withLoggingContext(extraValues) {
      if (logger.isInfoEnabled) logger.info(message)
    }

  /**
    * Logs a message with the `INFO` level.
    *
    * @param message the message to log
    * @param error   the associated exception
    */
  def info(message: => String, error: => Throwable, extraValues: (String, String)*)(
      implicit loggingContext: LoggingContext
  ): Unit =
    withLoggingContext(extraValues) {
      if (logger.isInfoEnabled) logger.info(message, error)
    }

  /**
    * Logs a message with the `WARN` level.
    *
    * @param message the message to log
    */
  def warn(message: => String)(implicit loggingContext: LoggingContext): Unit =
    withLoggingContext() {
      if (logger.isWarnEnabled) logger.warn(message)
    }

  /**
    * Logs a message with the `WARN` level.
    *
    * @param message the message to log
    * @param error   the associated exception
    */
  def warn(message: => String, error: => Throwable)(
      implicit loggingContext: LoggingContext
  ): Unit = withLoggingContext() {
    if (logger.isWarnEnabled) logger.warn(message, error)
  }

  /**
    * Logs a message with the `ERROR` level.
    *
    * @param message the message to log
    */
  def error(message: => String, extraValues: (String, String)*)(
      implicit loggingContext: LoggingContext
  ): Unit =
    withLoggingContext() {
      if (logger.isErrorEnabled) logger.error(message)
    }

  /**
    * Logs a message with the `ERROR` level.
    *
    * @param message the message to log
    * @param error   the associated exception
    */
  def error(message: => String, error: => Throwable, extraValues: (String, String)*)(
      implicit loggingContext: LoggingContext
  ): Unit = withLoggingContext() {
    if (logger.isErrorEnabled) logger.error(message, error)
  }

  /**
    * Log the outcome of a business try
    *
    * @param prefix  Log message prefix (e.g. business use case)
    * @param outcome the actual outcome
    */
  def logOutcome[R](prefix: String)(outcome: Try[DecidedBusinessTry[R]])(
      implicit loggingContext: LoggingContext
  ): Unit = withLoggingContext() {
    outcome match {
      case Success(BusinessSuccess(result)) =>
        businessDebug(s"$prefix : SUCCESS with $result")
      case Success(BusinessFailure(problem)) =>
        error(s"$prefix : FAILED with $problem")
      case Failure(exception) => error(s"$prefix : EXCEPTION", exception)
    }
  }

  def logProblem(message: String, problem: Problem)(
      implicit loggingContext: LoggingContext
  ): Unit = {
    if (problem.code < 500) {
      warn(s"Business problem $message: $problem")
    } else {
      error(s"Critical problem $message: $problem")
    }
  }
}
