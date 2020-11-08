package de.athalis.pass.parser.graphml

import java.io.File

import de.athalis.pass.model.TUDarmstadtModel.Process
import de.athalis.pass.parser.ast.pass.ProcessNode
import de.athalis.pass.parser.graphml.parser.GraphMLParser
import de.athalis.pass.parser.model.PASSModelMapper
import de.athalis.pass.processutil.base.PASSProcessReader

object PASSProcessReaderGraphML extends PASSProcessReader {

  private val fileExtension = ".graphml"

  override def canParseFile(file: File): Boolean = {
    file.getName.endsWith(fileExtension)
  }

  override def getFileExtensions: Set[String] = Set(fileExtension)

  override def parseProcesses(files: Set[File]): Set[Process] = {
    files.par.flatMap(file => {
      val processesAST: Set[ProcessNode] = GraphMLParser.loadProcesses(file).map(_._1)
      processesAST.map(PASSModelMapper.toPASSProcess)
    }).seq
  }

  override def parseProcesses(source: String, sourceName: String): Set[Process] = {
    val processesAST: Set[ProcessNode] = GraphMLParser.loadProcesses(source, sourceName).map(_._1)
    processesAST.map(PASSModelMapper.toPASSProcess)
  }

}
