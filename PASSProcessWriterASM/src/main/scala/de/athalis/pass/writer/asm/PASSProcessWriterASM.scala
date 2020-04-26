package de.athalis.pass.writer.asm

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import de.athalis.pass.model.TUDarmstadtModel.Process
import de.athalis.pass.model.TUDarmstadtModel.Types.ProcessIdentifier

import de.athalis.pass.processutil.base.PASSProcessWriter

import de.athalis.pass.writer.asm.Model2ASMMap.ProcessMap

object PASSProcessWriterASM extends PASSProcessWriter {

  private val fileExtension = ".casm"

  def write(processes: Set[Process], outDir: File): Set[File] = {
    val processesMap: Map[ProcessIdentifier, ProcessMap] = processes.map(p => {
      (p.identifier -> Model2ASMMap.toMap(p))
    }).toMap

    val writtenFiles = for((processID, processMap) <- processesMap) yield {
      val ruleName: String = "Load" + processID.replace(" ", "").replace(".", "_"); // TODO: use a safe `replace`

      outDir.mkdirs()
      val outFile: File = new File(outDir, processID + fileExtension)

      val asm: String = ASMCodeGenerator.toASMRule(ruleName, processID, processMap)

      val encoded = asm.getBytes(StandardCharsets.UTF_8)
      Files.write(outFile.toPath, encoded)

      outFile
    }

    writtenFiles.toSet
  }

}
