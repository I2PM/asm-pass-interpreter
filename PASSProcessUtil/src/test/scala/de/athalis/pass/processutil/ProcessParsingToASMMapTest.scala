package de.athalis.pass.processutil

import java.io.File

import de.athalis.pass.model.TUDarmstadtModel.{Examples, Process}
import de.athalis.pass.parser.PASSProcessReaderAST
import de.athalis.pass.writer.asm.Model2ASMMap
import org.scalatest.{FunSuite, Matchers}

class ProcessParsingToASMMapTest extends FunSuite with Matchers {

  test("emptyProcess") {
    // tests only if no exceptions occur

    val source = """Process Test {  }"""

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(source, "source", PASSProcessReaderAST)

    Model2ASMMap.toMap(processes)
  }

  test("processTestData") {
    // tests only if no exceptions occur
    val source = """Process Test { Data { "agents" -> {"foo1" -> {"bar1", "bar2"}, "foo2" -> {"bar3"}}, "emptySet" -> {}, "emptyMap" -> {->}, "foo1" -> "baa", "foo2" -> ["baz"], "foo3" -> [["bar1"], {"bar2", abc, 3}, {"bar3" -> "bar4", "bar5" -> "bar6"}] } }"""

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(source, "source", PASSProcessReaderAST)

    Model2ASMMap.toMap(processes)
  }

  test("load example Model map") {
    // tests only if no exceptions occur
    val examples = Set(Examples.travelRequestProcess, Examples.hotelBookingProcess)

    Model2ASMMap.toMap(examples)
  }


  val folder1 = new File("./processes/")
  val folder2 = new File("./asm/src/test/pass/")

  val files = FileUtils.listFiles(folder1) ++ FileUtils.listFiles(folder2)

  for(file <- files) {
    test("parses and maps: " + file) {
      // tests only if no exceptions occur
      val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(file)
      Model2ASMMap.toMap(processes)
    }
  }
}
