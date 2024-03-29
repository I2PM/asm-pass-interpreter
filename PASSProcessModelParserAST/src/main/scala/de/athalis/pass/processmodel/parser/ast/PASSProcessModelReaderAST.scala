package de.athalis.pass.processmodel.parser.ast

import de.athalis.pass.processmodel.PASSProcessModelCollection
import de.athalis.pass.processmodel.converter.ast.PASSModelMapper
import de.athalis.pass.processmodel.operation.PASSProcessModelReader
import de.athalis.pass.processmodel.parser.ast.node.pass.ProcessNode
import de.athalis.pass.processmodel.tudarmstadt.Process

import java.io.Reader
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher

import scala.collection.parallel.mutable.ParArray

object PASSProcessModelReaderAST extends PASSProcessModelReader[Process] {

  private val fileExtension = "*.pass"
  private val fileExtensionMatcher = FileSystems.getDefault.getPathMatcher("glob:**" + fileExtension)

  override def getPathMatcher: PathMatcher = fileExtensionMatcher

  override def canReadPath(path: Path): Boolean = {
    // TODO: check more / different than extension?
    Files.isReadable(path) && fileExtensionMatcher.matches(path)
  }

  override def readProcessModels(paths: Set[Path]): PASSProcessModelCollection[Process] = {
    val unreadable = paths.filterNot(canReadPath)
    if (unreadable.nonEmpty) {
      throw new IllegalArgumentException("unable to read source(s): " + unreadable)
    }

    val processModelsSet = ParArray.handoff[Path](paths.toArray).map(path => {
      val reader: Reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)
      readProcessModels(reader, path.getFileName.toString)
    }).seq.toSet

    PASSProcessModelCollection.flatten(processModelsSet)
  }

  override def readProcessModels(reader: Reader, sourceName: String): PASSProcessModelCollection[Process] = {
    val processModelsAST: Set[ProcessNode] = PASSParser.parseProcessModels(reader)
    val processModels: PASSProcessModelCollection[ProcessNode] = PASSProcessModelCollection.of(processModelsAST)
    PASSModelMapper.convert(processModels)
  }

}
