package microtools.logging

import org.slf4j.LoggerFactory

/**
 * Convenient trait to enable context aware logging.
 */
trait WithContextAwareLogger { self =>

  val log = new ContextAwareLogger(LoggerFactory.getLogger("application." + self.getClass.getName))
}
