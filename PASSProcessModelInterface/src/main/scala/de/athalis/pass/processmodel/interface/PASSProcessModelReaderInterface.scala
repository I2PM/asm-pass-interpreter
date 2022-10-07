package de.athalis.pass.processmodel.interface

import de.athalis.pass.processmodel.PASSProcessModelCollection
import de.athalis.pass.processmodel.interface.context.Analysis
import de.athalis.pass.processmodel.operation.PASSProcessModelReader
import de.athalis.pass.processmodel.tudarmstadt._

import java.io.File
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path

object PASSProcessModelReaderInterface {

  private def getReader(path: Path): PASSProcessModelReader[Process] = {
    val possibleReaders = Repository.readers.values.filter(_.canReadPath(path))

    if (possibleReaders.isEmpty) {
      throw new IllegalArgumentException(s"unable to find parser for file $path")
    }
    else if (possibleReaders.size > 1) {
      throw new Exception(s"multiple parsers found for file $path")
    }

    possibleReaders.head
  }

  def readProcessModels(rawPaths: String): PASSProcessModelCollection[Process] = {
    val paths: Set[Path] = rawPaths.split(File.pathSeparatorChar).map(filename => Path.of(filename)).toSet

    if (paths.isEmpty) {
      throw new IllegalArgumentException("no files given: " + rawPaths)
    }

    for (path <- paths) {
      if (!Files.isReadable(path)) {
        throw new IllegalArgumentException("cannot read file: " + path)
      }
    }

    readProcessModels(paths)
  }

  def readProcessModels(path: Path): PASSProcessModelCollection[Process] = {
    readProcessModels(Set(path))
  }

  def readProcessModels(paths: Set[Path]): PASSProcessModelCollection[Process] = {
    if (paths.isEmpty) {
      throw new IllegalArgumentException("no paths given")
    }

    val filesWithReader: Set[(Path, PASSProcessModelReader[Process])] = paths.map(path => (path, getReader(path)))

    val readersWithFiles: Iterable[(PASSProcessModelReader[Process], Set[Path])] = Repository.readers.values.map(reader => (reader, filesWithReader.filter(_._2 == reader).map(_._1)))

    val processModelsSet: Set[PASSProcessModelCollection[Process]] = readersWithFiles.map(t => {
      val reader = t._1
      val files = t._2
      reader.readProcessModels(files)
    }).toSet

    val processModels: PASSProcessModelCollection[Process] = PASSProcessModelCollection.flatten(processModelsSet)

    Analysis.convert(processModels)
  }

  def readProcessModels(sourceReader: Reader, sourceName: String, processModelReader: PASSProcessModelReader[Process]): PASSProcessModelCollection[Process] = {
    val processModels: PASSProcessModelCollection[Process] = processModelReader.readProcessModels(sourceReader, sourceName)

    Analysis.convert(processModels)
  }

}
