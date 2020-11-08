package de.athalis.pass.parser

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import de.athalis.pass.model.TUDarmstadtModel.Process
import de.athalis.pass.parser.ast.pass.ProcessNode
import de.athalis.pass.parser.model.PASSModelMapper
import de.athalis.pass.processutil.base.PASSProcessReader

object PASSProcessReaderAST extends PASSProcessReader {

  private val fileExtension = ".pass"

  override def getFileExtensions: Set[String] = Set(fileExtension)

  override def canParseFile(file: File): Boolean = {
    file.getName.endsWith(fileExtension)
  }

  override def parseProcesses(files: Set[File]): Set[Process] = {
    files.par.flatMap(file => {
      val source: String = Files.readString(file.toPath, StandardCharsets.UTF_8)
      parseProcesses(source, file.getName)
    }).seq
  }

  override def parseProcesses(source: String, sourceName: String): Set[Process] = {
    val processesNodes: Set[ProcessNode] = PASSParser.parseProcesses(source)
    processesNodes.map(PASSModelMapper.toPASSProcess)
  }

}
