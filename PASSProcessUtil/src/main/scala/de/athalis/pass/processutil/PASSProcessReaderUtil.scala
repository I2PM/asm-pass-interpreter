package de.athalis.pass.processutil

import java.io.File

import de.athalis.pass.model.TUDarmstadtModel._
import de.athalis.pass.parser.PASSProcessReaderAST
import de.athalis.pass.parser.graphml.PASSProcessReaderGraphML
import de.athalis.pass.processutil.base.{PASSProcessReader, PASSProcessWriter}
import de.athalis.pass.processutil.context.Analysis
import de.athalis.pass.writer.asm.PASSProcessWriterASM

object PASSProcessReaderUtil {

  private val readers: Seq[PASSProcessReader] = Seq(PASSProcessReaderAST, PASSProcessReaderGraphML)
  val fileExtensions: Set[String] = readers.toSet[PASSProcessReader].flatMap(_.getFileExtensions)

  private def getReader(file: File): Option[PASSProcessReader] = {
    readers.find(_.canParseFile(file))
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      throw new IllegalArgumentException("please specify file name as single argument")
    }

    val f = new File(args.head)

    if (!f.exists()) {
      throw new IllegalArgumentException(s"file '$f' not found")
    }

    val p: Set[Process] = readProcesses(f)

    println(p)
  }

  def readProcesses(file: File): Set[Process] = {
    val r: Option[PASSProcessReader] = getReader(file)

    if (r.isEmpty) {
      throw new IllegalArgumentException(s"unable to find parser for file ${file.getName}")
    }
    else {
      val processes: Set[Process] = r.get.parseProcesses(file)

      processes.map(Analysis.processAnalysisAndTransformation)
    }
  }

  def readProcesses(source: String, sourceName: String, reader: PASSProcessReader): Set[Process] = {
    val processes: Set[Process] = reader.parseProcesses(source, sourceName)

    processes.map(Analysis.processAnalysisAndTransformation)
  }

  def readAndWriteASM(inFile: File, outDir: File): Set[File] = readAndWrite(inFile, outDir, PASSProcessWriterASM)

  def readAndWrite(inFile: File, outDir: File, writer: PASSProcessWriter): Set[File] = {
    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(inFile)

    writer.write(processes, outDir)
  }
}
