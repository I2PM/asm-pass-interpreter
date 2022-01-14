package de.athalis.pass.processmodel.parser.ast.test

import de.athalis.pass.processmodel.parser.ast.PASSParser
import de.athalis.pass.processmodel.parser.ast.node.pass.ProcessNode

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ProcessTests extends AnyFunSuite with Matchers {
  import Util._

  test("processTestID") {
    val p: ProcessNode = parse(PASSParser.processNodeParser, """Process Foo { }""")

    p.id shouldBe "Foo"
  }

  test("processTestNamedDoubleQuote") {
    val p: ProcessNode = parse(PASSParser.processNodeParser, """Process "A B C" { }""")

    p.id shouldBe "A B C"
  }

  test("processTestNamedSingleQuote") {
    val p: ProcessNode = parse(PASSParser.processNodeParser, """Process 'A B C' { }""")

    p.id shouldBe "A B C"
  }

  test("processTest NoAgentManager") {
    val p: ProcessNode = parse(PASSParser.processNodeParser, """Process Foo { }""")

    p.getSubjects should have size 0

    p.getMacros should have size 0
  }

  test("processTestData") {
    val p: ProcessNode = parse(PASSParser.processNodeParser, """Process Test { Data { "emptySet" -> {}, "emptyMap" -> {->}, "foo1" -> "baa", "foo2" -> ["baz"], "foo3" -> [["bar1"], {"bar2", abc, 3}, {"bar3" -> "bar4", "bar5" -> "bar6"}]} }""")

    val data: Map[String, Any] = p.getData
    data should have size 5

    data.keys should contain ("emptySet")
    data.keys should contain ("emptyMap")
    data.keys should contain ("foo1")
    data.keys should contain ("foo2")
    data.keys should contain ("foo3")

    val emptySet = data("emptySet")
    emptySet shouldBe Set()

    val emptyMap = data("emptyMap")
    emptyMap shouldBe Map()

    val foo1 = data("foo1")
    foo1 shouldBe "baa"

    val foo2 = data("foo2")
    foo2 shouldBe Seq("baz")

    val foo3 = data("foo3")
    foo3 shouldBe Seq(
      Seq("bar1"),
      Set("bar2", "abc", 3),
      Map("bar3" -> "bar4", "bar5" -> "bar6"))

  }
}
