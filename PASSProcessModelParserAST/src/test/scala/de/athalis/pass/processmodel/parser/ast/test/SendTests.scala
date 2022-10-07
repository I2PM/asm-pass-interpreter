package de.athalis.pass.processmodel.parser.ast.test

import de.athalis.pass.processmodel.parser.ast.PASSParser
import de.athalis.pass.processmodel.parser.ast.node.pass.MacroNode
import de.athalis.pass.processmodel.parser.ast.util.ParserUtils

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SendTests extends AnyFunSuite with Matchers {
  import ParserUtils.parsePASSwithCause

  test("simple sending without var") {
    val m: MacroNode = parsePASSwithCause(PASSParser.macroParser,
      s"""Macro Foo {
         |  StartState := a
         |  a: Send ["foo" to X] -> TERMINATE
         |}""".stripMargin)

    val states = m.getStates
    val stateIDs = states.map(_.id)
    states should have size 2
    stateIDs should contain ("a")
    stateIDs should contain ("TERMINATE")

    val send = states.find(_.id == "a").get

    val sendOutTransitions = send.getOutgoingTransitions
    sendOutTransitions should have size 1

    val sendTransitions = send.getNormalOutgoingTransitions
    sendTransitions should have size 1

    val sendTransitionProperties = sendTransitions.head.communicationProperties.get
    sendTransitionProperties.subject shouldBe "X"
    sendTransitionProperties.subjectVar shouldBe ""
    sendTransitionProperties.subjectCountMin shouldBe 1
    sendTransitionProperties.subjectCountMax shouldBe 1
  }

  test("simple sending without var but pre-cancel") {
    val m: MacroNode = parsePASSwithCause(PASSParser.macroParser,
      s"""Macro Foo {
         |  StartState := a
         |  a: Send {
         |    "abbrechen" (cancel) -> TERMINATE
         |    ["foo" to X] -> TERMINATE
         |  }
         |}""".stripMargin)

    val states = m.getStates
    val stateIDs = states.map(_.id)
    states should have size 2
    stateIDs should contain ("a")
    stateIDs should contain ("TERMINATE")

    val send = states.find(_.id == "a").get

    val sendOutTransitions = send.getOutgoingTransitions
    sendOutTransitions should have size 2

    val sendTransitions = send.getNormalOutgoingTransitions
    sendTransitions should have size 1

    val sendTransitionProperties = sendTransitions.head.communicationProperties.get
    sendTransitionProperties.subject shouldBe "X"
    sendTransitionProperties.subjectVar shouldBe ""
    sendTransitionProperties.subjectCountMin shouldBe 1
    sendTransitionProperties.subjectCountMax shouldBe 1
  }

  test("simple sending without var but post-cancel") {
    val m: MacroNode = parsePASSwithCause(PASSParser.macroParser,
      s"""Macro Foo {
         |  StartState := a
         |  a: Send {
         |    ["foo" to X] -> TERMINATE
         |    "abbrechen" (cancel) -> TERMINATE
         |  }
         |}""".stripMargin)

    val states = m.getStates
    val stateIDs = states.map(_.id)
    states should have size 2
    stateIDs should contain ("a")
    stateIDs should contain ("TERMINATE")

    val send = states.find(_.id == "a").get


    val sendOutTransitions = send.getOutgoingTransitions
    sendOutTransitions should have size 2

    val sendTransitions = send.getNormalOutgoingTransitions
    sendTransitions should have size 1

    val sendTransitionProperties = sendTransitions.head.communicationProperties.get
    sendTransitionProperties.subject shouldBe "X"
    sendTransitionProperties.subjectVar shouldBe ""
    sendTransitionProperties.subjectCountMin shouldBe 1
    sendTransitionProperties.subjectCountMax shouldBe 1
  }

  test("simple sending with var") {
    val m: MacroNode = parsePASSwithCause(PASSParser.macroParser,
      s"""Macro Foo {
         |  StartState := a
         |  a: Send ["foo" to X in "bar"] -> TERMINATE
         |}""".stripMargin)

    val states = m.getStates
    val stateIDs = states.map(_.id)
    states should have size 2
    stateIDs should contain ("a")
    stateIDs should contain ("TERMINATE")

    val send = states.find(_.id == "a").get


    val sendOutTransitions = send.getOutgoingTransitions
    sendOutTransitions should have size 1

    val sendTransitions = send.getNormalOutgoingTransitions
    sendTransitions should have size 1

    val sendTransitionProperties = sendTransitions.head.communicationProperties.get
    sendTransitionProperties.subject shouldBe "X"
    sendTransitionProperties.subjectVar shouldBe "bar"
    sendTransitionProperties.subjectCountMin shouldBe 1
    sendTransitionProperties.subjectCountMax shouldBe 1
  }
}
