package de.athalis.pass.processmodel.interface

import de.athalis.pass.processmodel.PASSProcessModelCollection
import de.athalis.pass.processmodel.parser.ast.PASSProcessModelReaderAST
import de.athalis.pass.processmodel.tudarmstadt.Examples
import de.athalis.pass.processmodel.tudarmstadt.Process
import de.athalis.pass.processmodel.writer.asm.TUDarmstadtModel2ASMMap

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.io.StringReader

class ProcessParsingToASMMapMinimalTest extends AnyFunSuite with Matchers {

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

}
