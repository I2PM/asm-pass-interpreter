package de.athalis.pass.processmodel.parser.ast.test

import de.athalis.pass.processmodel.parser.ast.PASSParser
import de.athalis.pass.processmodel.parser.ast.node.pass.StateNode.StateType._
import de.athalis.pass.processmodel.parser.ast.util.ParserUtils

import org.scalatest.OptionValues._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class StateTests extends AnyFunSuite with Matchers {
  import ParserUtils.parsePASSwithCause

  test("stateTestSimpleID") {
    val s = parsePASSwithCause(PASSParser.states, """foo: InternalAction -> bar""")

    s.id shouldBe "foo"
    s.stateType shouldBe InternalAction
    s.priority shouldBe 0
  }

  test("stateTestSimpleNamed") {
    val s = parsePASSwithCause(PASSParser.states, """"foo bar": InternalAction -> bar""")

    s.id shouldBe "foo bar"
    s.stateType shouldBe InternalAction
    s.priority shouldBe 0
  }

  test("stateTestSimpleIDWithLabel") {
    val s = parsePASSwithCause(PASSParser.states, """foo "foo bar": InternalAction -> bar""")

    s.id shouldBe "foo"
    s.label.value shouldBe "foo bar"
    s.stateType shouldBe InternalAction
    s.priority shouldBe 0
  }

  test("stateTestFunction") {
    val s = parsePASSwithCause(PASSParser.states, """foo: "MyAction" (arg1, "arg2", 42, [1, 2], {3, 4}, {->}, {5 -> 6}, {[1, 2] -> {"a b" -> {3}}}) -> bar""")

    s.id shouldBe "foo"
    s.function shouldBe "MyAction"
    s.functionArguments shouldBe defined
    val args = s.functionArguments.get
    args should have size 8

    args(0) shouldBe "arg1"
    args(1) shouldBe "arg2"
    args(2) shouldBe 42
    args(3) shouldBe Seq(1, 2)
    args(4) shouldBe Set(3, 4)
    args(5) shouldBe Map()
    args(6) shouldBe Map(5 -> 6)
    args(7) shouldBe Map(Seq(1, 2) -> Map("a b" -> Set(3)))

    s.priority shouldBe 0
  }

  test("stateTestTwoTransitions") {
    val s = parsePASSwithCause(PASSParser.states, """foo: InternalAction {x -> bar y -> baz }""")

    s.id shouldBe "foo"
    s.stateType shouldBe InternalAction
    s.priority shouldBe 0
    s.getOutgoingTransitions should have size 2
  }

  test("stateTestPriority") {
    val s = parsePASSwithCause(PASSParser.states, """foo: InternalAction with priority 3 {x -> bar y -> baz }""")

    s.id shouldBe "foo"
    s.stateType shouldBe InternalAction
    s.priority shouldBe 3
    s.getOutgoingTransitions should have size 2
  }

  test("stateTestCallMacro") {
    val s = parsePASSwithCause(PASSParser.states, """foo: "CallMacro" ("X") { -> A -> B }""")

    s.id shouldBe "foo"
    s.stateType shouldBe FunctionState
    s.function shouldBe "CallMacro"
    s.functionArguments shouldBe defined
    val args = s.functionArguments.get
    args should have size 1
    args should contain ("X")
    s.getOutgoingTransitions should have size 2
  }

  test("stateTestTerminateWithArguments") {
    val s = parsePASSwithCause(PASSParser.states, """foo: Terminate ("x")""")

    s.id shouldBe "foo"
    s.stateType shouldBe Terminate
    s.function shouldBe ""
    s.functionArguments shouldBe defined
    val args = s.functionArguments.get
    args should have size 1
    args should contain ("x")
    s.getOutgoingTransitions should have size 0
  }

  test("stateTestReturnWithArguments") {
    val s = parsePASSwithCause(PASSParser.states, """foo: Return ("x")""")

    s.id shouldBe "foo"
    s.stateType shouldBe Return
    s.function shouldBe ""
    s.functionArguments shouldBe defined
    val args = s.functionArguments.get
    args should have size 1
    args should contain ("x")
    s.getOutgoingTransitions should have size 0
  }

  test("stateTests") {
    parsePASSwithCause(PASSParser.states, """x:  InternalAction         -> nextState""")
    parsePASSwithCause(PASSParser.states, """x: "MyAction"              -> nextState""")
    parsePASSwithCause(PASSParser.states, """x:  InternalAction   "foo" -> nextState""")
    parsePASSwithCause(PASSParser.states, """x:  InternalAction  {      -> nextState }""")
    parsePASSwithCause(PASSParser.states, """x:  InternalAction  {"foo" -> nextState }""")

    parsePASSwithCause(PASSParser.states, """x:  InternalAction  (auto) -> nextState""")
    parsePASSwithCause(PASSParser.states, """x:  "MyAction"      (auto) -> nextState""")

    parsePASSwithCause(PASSParser.states, """x:  InternalAction   ["foo" to A]   -> nextState""")
    parsePASSwithCause(PASSParser.states, """x:  InternalAction   ["foo" from A] -> nextState""")
    parsePASSwithCause(PASSParser.states, """x:  InternalAction  {["foo" from A] -> nextState }""")
    parsePASSwithCause(PASSParser.states, """x:  InternalAction  {["foo" from A] -> nextState ["bar" from A] -> nextState }""")

    parsePASSwithCause(PASSParser.states, """x:  InternalAction  ["foo" to A in "var"] -> nextState""")
  }
}
