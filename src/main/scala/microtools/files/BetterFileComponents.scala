package microtools.files

import play.api.inject.ApplicationLifecycle
import play.api.libs.Files._

trait BetterFileComponents {
  def applicationLifecycle: ApplicationLifecycle

  def tempFileReaper: TemporaryFileReaper

  lazy val betterTemporaryFileCreator: BetterTemporaryFileCreator =
    new BetterTemporaryFileCreator(applicationLifecycle, tempFileReaper)
}
