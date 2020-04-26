package de.athalis.pass.parser.test

import org.scalatest.{FunSuite, Matchers}

import de.athalis.pass.parser.PASSParser
import de.athalis.pass.parser.ast.pass._

class MacroTests extends FunSuite with Matchers {
  import Util._

  test("macroTestID") {
    val m: MacroNode = parse(PASSParser.macroParser, "Macro Foo { }")

    m.id shouldBe "Foo"
  }

  test("macroTestNamed") {
    val m: MacroNode = parse(PASSParser.macroParser, "Macro \"Foo Bar\" { }")

    m.id shouldBe "Foo Bar"
  }

  test("macroTestVariables") {
    val m: MacroNode = parse(PASSParser.macroParser, "Macro Foo { LocalVariables := {\"bar\", \"foo\", \"bar\"} }")

    val macroVariables = m.getMacroVariables
    macroVariables should have size 2
    macroVariables should contain ("bar")
    macroVariables should contain ("foo")
  }

  test("macroTestNoStartState") {
    val m: MacroNode = parse(PASSParser.macroParser, "Macro Foo { StartState := Baz }")

    an[NoSuchElementException] should be thrownBy {
      m.getStartStateNumber
    }
  }

  test("macroTestStartState") {
    val m: MacroNode = parse(PASSParser.macroParser, "Macro Foo { StartState := a a: InternalAction -> b b: InternalAction -> END }")

    val states = m.getStates
    val stateIDs = states.map(_.id)
    states should have size 3
    stateIDs should contain ("a")
    stateIDs should contain ("b")
    stateIDs should contain ("END")

    val stateInternalIDs = states.map(_.stateNumber)
    stateInternalIDs should contain (m.getStartStateNumber)
  }
}
