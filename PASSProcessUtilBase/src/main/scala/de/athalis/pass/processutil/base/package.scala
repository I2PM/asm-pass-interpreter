package de.athalis.pass.processutil

import java.io.File

import de.athalis.pass.model.TUDarmstadtModel.Process

package object base {

  trait PASSProcessReader {
    def getFileExtensions: Set[String]
    def canParseFile(file: File): Boolean

    def parseProcesses(file: File): Set[Process]
    def parseProcesses(source: String, sourceName: String): Set[Process]
  }

  trait PASSProcessWriter {
    def write(processes: Set[Process], outDir: File): Set[File]
  }

}
