package de.athalis.pass.parser.test

import org.scalatest.{FunSuite, Matchers}
import org.scalatest.OptionValues._

import de.athalis.pass.parser.PASSParser
import de.athalis.pass.parser.ast.pass.StateNode.StateType._

class StateTests extends FunSuite with Matchers {
  import Util._

  test("stateTestSimpleID") {
    val s = parse(PASSParser.states, """foo: InternalAction -> bar""")

    s.id shouldBe "foo"
    s.stateType shouldBe InternalAction
    s.priority shouldBe 0
  }

  test("stateTestSimpleNamed") {
    val s = parse(PASSParser.states, """"foo bar": InternalAction -> bar""")

    s.id shouldBe "foo bar"
    s.stateType shouldBe InternalAction
    s.priority shouldBe 0
  }

  test("stateTestSimpleIDWithLabel") {
    val s = parse(PASSParser.states, """foo "foo bar": InternalAction -> bar""")

    s.id shouldBe "foo"
    s.label.value shouldBe "foo bar"
    s.stateType shouldBe InternalAction
    s.priority shouldBe 0
  }

  test("stateTestFunction") {
    val s = parse(PASSParser.states, """foo: "MyAction" (arg1, "arg2", 42, [1, 2], {3, 4}, {->}, {5 -> 6}, {[1, 2] -> {"a b" -> {3}}}) -> bar""")

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
    val s = parse(PASSParser.states, """foo: InternalAction {x -> bar y -> baz }""")

    s.id shouldBe "foo"
    s.stateType shouldBe InternalAction
    s.priority shouldBe 0
    s.getOutgoingTransitions should have size 2
  }

  test("stateTestPriority") {
    val s = parse(PASSParser.states, """foo: InternalAction with priority 3 {x -> bar y -> baz }""")

    s.id shouldBe "foo"
    s.stateType shouldBe InternalAction
    s.priority shouldBe 3
    s.getOutgoingTransitions should have size 2
  }

  test("stateTestCallMacro") {
    val s = parse(PASSParser.states, """foo: "CallMacro" ("X") { -> A -> B }""")

    s.id shouldBe "foo"
    s.stateType shouldBe FunctionState
    s.function shouldBe "CallMacro"
    s.functionArguments shouldBe defined
    val args = s.functionArguments.get
    args should have size 1
    args should contain ("X")
    s.getOutgoingTransitions should have size 2
  }

  test("stateTestEndWithArguments") {
    val s = parse(PASSParser.states, """foo: End ("x")""")

    s.id shouldBe "foo"
    s.stateType shouldBe End
    s.function shouldBe ""
    s.functionArguments shouldBe defined
    val args = s.functionArguments.get
    args should have size 1
    args should contain ("x")
    s.getOutgoingTransitions should have size 0
  }

  test("stateTests") {
    parse(PASSParser.states, """x:  InternalAction         -> nextState""")
    parse(PASSParser.states, """x: "MyAction"              -> nextState""")
    parse(PASSParser.states, """x:  InternalAction   "foo" -> nextState""")
    parse(PASSParser.states, """x:  InternalAction  {      -> nextState }""")
    parse(PASSParser.states, """x:  InternalAction  {"foo" -> nextState }""")

    parse(PASSParser.states, """x:  InternalAction  (auto) -> nextState""")
    parse(PASSParser.states, """x:  "MyAction"      (auto) -> nextState""")

    parse(PASSParser.states, """x:  InternalAction   ["foo" to A]   -> nextState""")
    parse(PASSParser.states, """x:  InternalAction   ["foo" from A] -> nextState""")
    parse(PASSParser.states, """x:  InternalAction  {["foo" from A] -> nextState }""")
    parse(PASSParser.states, """x:  InternalAction  {["foo" from A] -> nextState ["bar" from A] -> nextState }""")

    parse(PASSParser.states, """x:  InternalAction  ["foo" to A in "var"] -> nextState""")
  }
}
