package de.athalis.pass.processmodel.operation

import de.athalis.pass.processmodel.PASSProcessModel
import de.athalis.pass.processmodel.PASSProcessModelCollection

import java.io.Reader
import java.nio.file.Path
import java.nio.file.PathMatcher

trait PASSProcessModelReader[T <: PASSProcessModel] {
  def getPathMatcher: PathMatcher
  def canReadPath(path: Path): Boolean

  def readProcessModels(paths: Set[Path]): PASSProcessModelCollection[T]
  def readProcessModels(reader: Reader, sourceName: String): PASSProcessModelCollection[T]
}
