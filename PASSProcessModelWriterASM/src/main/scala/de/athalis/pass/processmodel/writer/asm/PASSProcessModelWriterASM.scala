package de.athalis.pass.processmodel.writer.asm

import de.athalis.pass.processmodel.PASSProcessModelCollection
import de.athalis.pass.processmodel.operation.PASSProcessModelWriter
import de.athalis.pass.processmodel.tudarmstadt.Process
import de.athalis.pass.processmodel.writer.asm.TUDarmstadtModel2ASMMap.ProcessModelsASMMap

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object PASSProcessModelWriterASM extends PASSProcessModelWriter[Process] {

  private val fileExtension = ".casm"

  def write(processModels: PASSProcessModelCollection[Process], outDir: Path): Set[Path] = {
    val processModelsConverted: PASSProcessModelCollection[ProcessModelASMMapWrapper] = TUDarmstadtModel2ASMMap.convert(processModels)
    val processModelsMap: ProcessModelsASMMap = ProcessModelASMMapWrapper.toProcessModelsASMMap(processModelsConverted)

    val writtenFiles = for((processModelID, processModelMap) <- processModelsMap) yield {
      val ruleName: String = "Load" + processModelID.replace(" ", "").replace(".", "_"); // TODO: use a safe `replace`

      Files.createDirectories(outDir)
      val outFile: Path = outDir.resolve(processModelID + fileExtension)

      val asm: String = ASMCodeGenerator.toASMRule(ruleName, processModelID, processModelMap)

      val encoded = asm.getBytes(StandardCharsets.UTF_8)
      Files.write(outFile, encoded)

      outFile
    }

    writtenFiles.toSet
  }

}
