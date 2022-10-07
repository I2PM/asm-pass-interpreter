package de.athalis.pass.processmodel.interface

import de.athalis.pass.processmodel.PASSProcessModelCollection
import de.athalis.pass.processmodel.tudarmstadt.Process
import de.athalis.pass.processmodel.writer.asm.TUDarmstadtModel2ASMMap

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.nio.file.Path

class ProcessParsingToASMMapFullTest extends AnyFunSuite with Matchers {
  val folder1 = Path.of("processes")
  val folder2 = Path.of("asm", "src", "test", "pass")

  val files = FileUtils.listProcessModelFiles(folder1) ++ FileUtils.listProcessModelFiles(folder2)

  for(file <- files) {
    test("parses and maps: " + file) {
      // tests only if no exceptions occur
      val processModels: PASSProcessModelCollection[Process] = PASSProcessModelReaderInterface.readProcessModels(file)
      TUDarmstadtModel2ASMMap.convert(processModels)
    }
  }

}
