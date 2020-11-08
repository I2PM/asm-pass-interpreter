package de.athalis.pass.processutil

import java.io.File

import de.athalis.pass.model.TUDarmstadtModel._
import de.athalis.pass.parser.PASSProcessReaderAST
import de.athalis.pass.parser.graphml.PASSProcessReaderGraphML
import de.athalis.pass.processutil.base.PASSProcessReader
import de.athalis.pass.processutil.context.Analysis

object PASSProcessReaderUtil {

  private val readers: Seq[PASSProcessReader] = Seq(PASSProcessReaderAST, PASSProcessReaderGraphML)
  val fileExtensions: Set[String] = readers.toSet[PASSProcessReader].flatMap(_.getFileExtensions)

  private def getReader(file: File): PASSProcessReader = {
    val possibleReaders = readers.filter(_.canParseFile(file))

    if (possibleReaders.isEmpty) {
      throw new IllegalArgumentException(s"unable to find parser for file ${file.getName}")
    }
    else if (possibleReaders.size > 1) {
      throw new Exception(s"multiple parsers found for file ${file.getName}")
    }

    possibleReaders.head
  }

  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      throw new IllegalArgumentException(s"please specify file names, separated by '${File.pathSeparatorChar}'")
    }

    val paths = args.mkString(" ")

    val p: Set[Process] = readProcesses(paths)

    println(p)
  }

  def readProcesses(paths: String): Set[Process] = {
    val files: Set[File] = paths.split(File.pathSeparatorChar).map(filename => new File(filename)).toSet

    if (files.isEmpty) {
      throw new IllegalArgumentException("no files given: " + paths)
    }

    for (file <- files) {
      if (!file.isFile) {
        throw new IllegalArgumentException("not a file: " + file)
      }
    }

    val processes: Set[Process] = readProcesses(files)

    processes
  }
  def readProcesses(file: File): Set[Process] = {
    readProcesses(Set(file))
  }

  def readProcesses(files: Set[File]): Set[Process] = {
    if (files.isEmpty) {
      throw new IllegalArgumentException("no files given")
    }

    val filesWithReader: Set[(File, PASSProcessReader)] = files.map(file => (file, getReader(file)))

    val readersWithFiles: Set[(PASSProcessReader, Set[File])] = readers.toSet[PASSProcessReader].map(reader => (reader, filesWithReader.filter(_._2 == reader).map(_._1)))

    val processes: Set[Process] = readersWithFiles.flatMap(t => {
      val reader = t._1
      val files = t._2
      reader.parseProcesses(files)
    })

    processes.map(Analysis.processAnalysisAndTransformation)
  }

  def readProcesses(source: String, sourceName: String, reader: PASSProcessReader): Set[Process] = {
    val processes: Set[Process] = reader.parseProcesses(source, sourceName)

    processes.map(Analysis.processAnalysisAndTransformation)
  }
}
