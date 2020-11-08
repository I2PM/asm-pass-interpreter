package de.athalis.pass.parser.util

import de.athalis.pass.parser.ast._

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ParserUtilsSpec extends AnyFunSuite with Matchers {

  test("nodeToStringTest") {
    val id = new IDNode("test")
    id.value shouldBe "test"

    val str = new STRINGNode("test")
    str.value shouldBe "test"
  }

  test("nodeToIntTest") {
    val num = new NUMBERNode(12)
    num.value shouldBe 12
  }


  test("nodeToBooleanTestTrue") {
    val x = BOOLEANNode.PARSER(KWNode("true"))

    x.value shouldBe true
  }

  test("nodeToBooleanTestFalse") {
    val x = BOOLEANNode.PARSER(KWNode("false"))

    x.value shouldBe false
  }

  test("nodeToBooleanTestFail") {
    an[IllegalArgumentException] should be thrownBy {
      BOOLEANNode.PARSER(KWNode("something"))
    }
  }


  test("NodeDebugger") {
    val id = new IDNode("testId")
    val str = new STRINGNode("testString")
    val seq = Seq(new STRINGNode("test1"), new STRINGNode("test2"))

    val a1: Array[_] = Seq(id, str, seq).toArray
    NodeDebugger.trace(a1)
  }
}
