package de.athalis.pass.processmodel.parser.graphml

import de.athalis.pass.processmodel.PASSProcessModelCollection
import de.athalis.pass.processmodel.converter.ast.PASSModelMapper
import de.athalis.pass.processmodel.parser.ast.node.pass.ProcessNode
import de.athalis.pass.processmodel.parser.graphml.parser.GraphMLParser
import de.athalis.pass.processmodel.tudarmstadt.Process
import de.athalis.pass.processutil.base.PASSProcessModelReader

import java.io.Reader
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher

object PASSProcessModelReaderGraphML extends PASSProcessModelReader {

  private val fileExtension = ".graphml"
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

    val processModelsSet: Set[PASSProcessModelCollection[Process]] = paths.par.map(path => {
      val processModelsAST: Set[ProcessNode] = GraphMLParser.loadProcessModels(path).map(_._1)
      val processModels: PASSProcessModelCollection[ProcessNode] = PASSProcessModelCollection.of(processModelsAST)
      PASSModelMapper.convert(processModels)
    }).seq

    PASSProcessModelCollection.flatten(processModelsSet)
  }

  override def readProcessModels(reader: Reader, sourceName: String): PASSProcessModelCollection[Process] = {
    val processModelsAST: Set[ProcessNode] = GraphMLParser.loadProcessModels(reader, sourceName).map(_._1)
    val processModels: PASSProcessModelCollection[ProcessNode] = PASSProcessModelCollection.of(processModelsAST)
    PASSModelMapper.convert(processModels)
  }

}
