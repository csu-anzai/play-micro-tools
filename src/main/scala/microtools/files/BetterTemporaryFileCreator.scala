package microtools.files

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}

import javax.inject.{Inject, Singleton}
import play.api.inject.ApplicationLifecycle
import play.api.libs.Files.TemporaryFileReaper

import scala.concurrent.Future

@Singleton
class BetterTemporaryFileCreator @Inject()(temporaryFileReaper: TemporaryFileReaper,
                                           applicationLifecycle: ApplicationLifecycle) {
  private val tempDirectory = {
    val tempDir = Files.createTempDirectory("betterTempDir")
    temporaryFileReaper.updateTempFolder(tempDir)

    tempDir
  }

  def create(prefix: String, suffix: String): Path =
    Files.createTempFile(tempDirectory, prefix, suffix)

  applicationLifecycle.addStopHook { () =>
    Future.successful(
      Files.walkFileTree(
        tempDirectory,
        new SimpleFileVisitor[Path] {
          override def visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult = {
            Files.deleteIfExists(path)

            FileVisitResult.CONTINUE
          }

          override def postVisitDirectory(path: Path, exc: IOException): FileVisitResult = {
            Files.deleteIfExists(path)

            FileVisitResult.CONTINUE
          }
        }
      ))
  }
}
