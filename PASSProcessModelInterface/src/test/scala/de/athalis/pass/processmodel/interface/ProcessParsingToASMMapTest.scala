package de.athalis.pass.processmodel.interface

import de.athalis.pass.processmodel.PASSProcessModelCollection
import de.athalis.pass.processmodel.parser.ast.PASSProcessModelReaderAST
import de.athalis.pass.processmodel.tudarmstadt.Examples
import de.athalis.pass.processmodel.tudarmstadt.Process
import de.athalis.pass.processmodel.writer.asm.TUDarmstadtModel2ASMMap

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.io.StringReader
import java.nio.file.Path

class ProcessParsingToASMMapTest extends AnyFunSuite with Matchers {

  test("emptyProcess") {
    // tests only if no exceptions occur

    val source = """Process Test {  }"""

    val processModels: PASSProcessModelCollection[Process] = PASSProcessModelReaderInterface.readProcessModels(new StringReader(source), "source", PASSProcessModelReaderAST)

    TUDarmstadtModel2ASMMap.convert(processModels)
  }

  test("processTestData") {
    // tests only if no exceptions occur
    val source =
      """
        | Process Test {
        |   Data {
        |     "agents" -> {"foo1" -> {"bar1", "bar2"}, "foo2" -> {"bar3"}},
        |     "emptySet" -> {},
        |     "emptyMap" -> {->},
        |     "foo1" -> "baa",
        |     "foo2" -> ["baz"],
        |     "foo3" -> [
        |       ["bar1"],
        |       {"bar2", abc, 3},
        |       {"bar3" -> "bar4", "bar5" -> "bar6"}
        |     ]
        |   }
        | }""".stripMargin

    val processModels: PASSProcessModelCollection[Process] = PASSProcessModelReaderInterface.readProcessModels(new StringReader(source), "source", PASSProcessModelReaderAST)

    TUDarmstadtModel2ASMMap.convert(processModels)
  }

  test("load example Model map") {
    // tests only if no exceptions occur
    val examples = PASSProcessModelCollection(Examples.travelRequestProcess, Examples.hotelBookingProcess)

    TUDarmstadtModel2ASMMap.convert(examples)
  }


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
