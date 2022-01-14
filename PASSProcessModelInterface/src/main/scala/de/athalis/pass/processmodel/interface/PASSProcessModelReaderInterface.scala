package de.athalis.pass.processmodel.interface

import de.athalis.pass.processmodel.PASSProcessModelCollection
import de.athalis.pass.processmodel.interface.context.Analysis
import de.athalis.pass.processmodel.parser.ast.PASSProcessModelReaderAST
import de.athalis.pass.processmodel.parser.graphml.PASSProcessModelReaderGraphML
import de.athalis.pass.processmodel.tudarmstadt._
import de.athalis.pass.processutil.base.PASSProcessModelReader

import java.io.File
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher

object PASSProcessModelReaderInterface {

  private val readers: Set[PASSProcessModelReader] = Set(PASSProcessModelReaderAST, PASSProcessModelReaderGraphML)
  val pathMatchers: Set[PathMatcher] = readers.map(_.getPathMatcher)

  private def getReader(path: Path): PASSProcessModelReader = {
    val possibleReaders = readers.filter(_.canReadPath(path))

    if (possibleReaders.isEmpty) {
      throw new IllegalArgumentException(s"unable to find parser for file $path")
    }
    else if (possibleReaders.size > 1) {
      throw new Exception(s"multiple parsers found for file $path")
    }

    possibleReaders.head
  }

  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      throw new IllegalArgumentException(s"please specify file names, separated by '${File.pathSeparatorChar}'")
    }

    val paths = args.mkString(" ")

    val processModels: PASSProcessModelCollection[Process] = readProcessModels(paths)

    println(processModels)
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

    val filesWithReader: Set[(Path, PASSProcessModelReader)] = paths.map(path => (path, getReader(path)))

    val readersWithFiles: Set[(PASSProcessModelReader, Set[Path])] = readers.map(reader => (reader, filesWithReader.filter(_._2 == reader).map(_._1)))

    val processModelsSet: Set[PASSProcessModelCollection[Process]] = readersWithFiles.map(t => {
      val reader = t._1
      val files = t._2
      reader.readProcessModels(files)
    })

    val processModels: PASSProcessModelCollection[Process] = PASSProcessModelCollection.flatten(processModelsSet)

    Analysis.convert(processModels)
  }

  def readProcessModels(sourceReader: Reader, sourceName: String, processModelReader: PASSProcessModelReader): PASSProcessModelCollection[Process] = {
    val processModels: PASSProcessModelCollection[Process] = processModelReader.readProcessModels(sourceReader, sourceName)

    Analysis.convert(processModels)
  }
}
