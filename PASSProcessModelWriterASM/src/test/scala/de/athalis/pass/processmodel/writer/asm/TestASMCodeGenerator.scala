package de.athalis.pass.processmodel.writer.asm

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TestASMCodeGenerator extends AnyFunSuite with Matchers {

  test("ASMCodeGenerator") {
    val writer = new StringBuilder()
    ASMCodeGenerator.appendASMCode(writer, Map(1 -> "a", 2 -> Seq(1.0, Set("a"), true))).toString shouldBe
      """{
        |1 -> "a",
        |2 -> [1.0, {"a"}, true]
        |}
        |""".stripMargin
  }

}
