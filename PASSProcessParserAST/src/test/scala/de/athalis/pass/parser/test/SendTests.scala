package de.athalis.pass.parser.test

import org.scalatest.{FunSuite, Matchers}

import de.athalis.pass.parser.PASSParser
import de.athalis.pass.parser.ast.pass._

class SendTests extends FunSuite with Matchers {
  import Util._

  test("simple sending without var") {
    val m: MacroNode = parse(PASSParser.macroParser,
      s"""Macro Foo {
         |  StartState := a
         |  a: Send ["foo" to X] -> END
         |}""".stripMargin)

    val states = m.getStates
    val stateIDs = states.map(_.id)
    states should have size 2
    stateIDs should contain ("a")
    stateIDs should contain ("END")

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
    val m: MacroNode = parse(PASSParser.macroParser,
      s"""Macro Foo {
         |  StartState := a
         |  a: Send {
         |    "abbrechen" (cancel) -> END
         |    ["foo" to X] -> END
         |  }
         |}""".stripMargin)

    val states = m.getStates
    val stateIDs = states.map(_.id)
    states should have size 2
    stateIDs should contain ("a")
    stateIDs should contain ("END")

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
    val m: MacroNode = parse(PASSParser.macroParser,
      s"""Macro Foo {
         |  StartState := a
         |  a: Send {
         |    ["foo" to X] -> END
         |    "abbrechen" (cancel) -> END
         |  }
         |}""".stripMargin)

    val states = m.getStates
    val stateIDs = states.map(_.id)
    states should have size 2
    stateIDs should contain ("a")
    stateIDs should contain ("END")

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
    val m: MacroNode = parse(PASSParser.macroParser,
      s"""Macro Foo {
         |  StartState := a
         |  a: Send ["foo" to X in "bar"] -> END
         |}""".stripMargin)

    val states = m.getStates
    val stateIDs = states.map(_.id)
    states should have size 2
    stateIDs should contain ("a")
    stateIDs should contain ("END")

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
