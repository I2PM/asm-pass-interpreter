package de.athalis.pass.processmodel.interface

import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher

import scala.collection.JavaConverters._

object FileUtils {

  def listProcessModelFiles(path: Path): Set[Path] = listFiles(path, processModelPathMatchers)

  def listFiles(path: Path, filter: DirectoryStream.Filter[Path]): Set[Path] = {
    val ds = Files.newDirectoryStream(path, filter)
    val r = ds.asScala.toSet
    ds.close()
    r
  }

  class FileExtensionFilter(fileExtensions: Set[PathMatcher]) extends DirectoryStream.Filter[Path] {
    override def accept(path: Path): Boolean = {
      fileExtensions.exists(_.matches(path))
    }
  }

  private val processModelPathMatchers: DirectoryStream.Filter[Path] = new FileExtensionFilter(PASSProcessModelReaderInterface.pathMatchers)

}
