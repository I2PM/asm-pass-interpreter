package de.athalis.pass.parser.test

import org.jparsec.error.ParserException

import de.athalis.pass.parser.PASSParser
import de.athalis.pass.parser.ast.pass.TransitionNode.TransitionType._

import org.scalatest.OptionValues._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TransitionTests extends AnyFunSuite with Matchers {
  import Util._

  test("countMinMaxParserCount") {
    val c = parse(PASSParser.countMinMaxParser, """3""")

    c.min shouldBe 3
    c.maxO.value shouldBe 3
    c.max shouldBe 3
  }

  test("countMinMaxParserMinFail") {
    an[ParserException] should be thrownBy {
      parse(PASSParser.countMinMaxParser, """min 3""")
    }
  }

  test("countMinMaxParserMin?Fail") {
    an[ParserException] should be thrownBy {
      parse(PASSParser.countMinMaxParser, """min 3 max ?""")
    }
  }

  test("countMinMaxParserMin*") {
    val c = parse(PASSParser.countMinMaxParser, """min 3 max *""")

    c.min shouldBe 3
    c.maxO shouldBe None
    c.max shouldBe 0
  }

  test("countMinMaxParserMaxFail") {
    an[ParserException] should be thrownBy {
      parse(PASSParser.countMinMaxParser, """max 3""")
    }
  }

  test("countMinMaxParserMaxFail*") {
    an[ParserException] should be thrownBy {
      parse(PASSParser.countMinMaxParser, """min * max 3""")
    }
  }

  test("countMinMaxParserMaxFail?") {
    an[ParserException] should be thrownBy {
      parse(PASSParser.countMinMaxParser, """min ? max 3""")
    }
  }

  test("countMinMaxParserMinMax") {
    val c = parse(PASSParser.countMinMaxParser, """min 5 max 10""")

    c.min shouldBe 5
    c.maxO.value shouldBe 10
    c.max shouldBe 10
  }


  test("transitionTestBasicToID") {
    val e = parse(PASSParser.transition, """-> nextState""")

    e.label should not be 'defined
    e.getType shouldBe Normal
    e.targetStateID shouldBe "nextState"
    e.isAuto shouldBe false
  }

  test("transitionTestBasicToName") {
    val e = parse(PASSParser.transition, """-> "next state"""")

    e.label should not be 'defined
    e.getType shouldBe Normal
    e.targetStateID shouldBe "next state"
    e.isAuto shouldBe false
  }

  test("transitionTestNamedToID") {
    val e = parse(PASSParser.transition, """"transition name" -> nextState""")

    e.label shouldBe Some("transition name")
    e.getType shouldBe Normal
    e.targetStateID shouldBe "nextState"
    e.isAuto shouldBe false
  }

  test("transitionTestMessageTo") {
    val e = parse(PASSParser.transition, """["msg type" to A] -> nextState""")

    e.label should not be 'defined
    e.getType shouldBe Normal
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "A"
    e.isAuto shouldBe false
  }

  test("transitionTestMessageToWithName") {
    val e = parse(PASSParser.transition, """"transition name" ["msg type" to A] -> nextState""")

    e.label shouldBe Some("transition name")
    e.getType shouldBe Normal
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "A"
    e.isAuto shouldBe false
  }

  test("transitionTestAutoWithName") {
    val e = parse(PASSParser.transition, """"transition name" (auto) -> nextState""")

    e.label shouldBe Some("transition name")
    e.getType shouldBe Normal
    e.targetStateID shouldBe "nextState"
    e.isAuto shouldBe true
  }

  test("transitionTestHiddenWithName") {
    val e = parse(PASSParser.transition, """"transition name" (hidden) -> nextState""")

    e.label shouldBe Some("transition name")
    e.getType shouldBe Normal
    e.targetStateID shouldBe "nextState"
    e.isHidden shouldBe true
  }

  test("transitionTestAutoWithNameAndPriority") {
    val e = parse(PASSParser.transition, """"transition name" (auto with priority 2) -> nextState""")

    e.label shouldBe Some("transition name")
    e.getType shouldBe Normal
    e.targetStateID shouldBe "nextState"
    e.isAuto shouldBe true
    e.getPriority shouldBe 2
  }

  test("transitionTestCancelWithoutName") {
    val e = parse(PASSParser.transition, "(cancel) -> nextState")

    e.getType shouldBe Cancel
    e.targetStateID shouldBe "nextState"
    e.isAuto shouldBe false
  }

  test("transitionTestCancelWithName") {
    val e = parse(PASSParser.transition, """"transition name" (cancel) -> nextState""")

    e.label shouldBe Some("transition name")
    e.getType shouldBe Cancel
    e.targetStateID shouldBe "nextState"
    e.isAuto shouldBe false
  }

  test("transitionTestTimeoutManualWithName fail without time") {
    an[ParserException] shouldBe thrownBy {
      parse(PASSParser.transition, """"transition name" (timeout) -> nextState""")
    }
  }

  test("transitionTestTimeoutManualWithTimeWithName") {
    val e = parse(PASSParser.transition, """"transition name" (timeout(120)) -> nextState""")

    e.label shouldBe Some("transition name")
    e.getType shouldBe Timeout
    e.targetStateID shouldBe "nextState"
    e.getTimeout shouldBe 120
    e.isAuto shouldBe false
  }

  // note: timeouts are always handled as auto
  test("transitionTestTimeoutAutoWithTimeWithName") {
    val e = parse(PASSParser.transition, """"transition name" (auto timeout(120)) -> nextState""")

    e.label shouldBe Some("transition name")
    e.getType shouldBe Timeout
    e.targetStateID shouldBe "nextState"
    e.isAuto shouldBe true
    e.getTimeout shouldBe 120
  }

  test("transitionTestDollarSingleQuote") {
    val e = parse(PASSParser.transition, """'$foo and \$bar' -> nextState""")

    e.label shouldBe Some("""$foo and \$bar""")
    e.targetStateID shouldBe "nextState"
  }

  test("transitionTestDollarDoubleQuote") {
    val e = parse(PASSParser.transition, """"$foo and \\$bar" -> nextState""")

    e.label shouldBe Some("""$foo and \$bar""")
    e.targetStateID shouldBe "nextState"
  }

  test("transitionTestMessageFrom") {
    val e = parse(PASSParser.transition, """["msg type" from B] -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "B"
    e.isAuto shouldBe false
  }

  test("transitionTestMessageFromMulti") {
    val e = parse(PASSParser.transition, """["msg type" from 3 of B] -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "B"
    e.communicationProperties.value.subjectCountMin shouldBe 3
    e.communicationProperties.value.subjectCountMax shouldBe 3
    e.isAuto shouldBe false
  }

  test("transitionTestReceiveAnyWithAnyFromAny") {
    val e = parse(PASSParser.transition, """[? with correlation of ? from ?] -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "?"
    e.communicationProperties.value.with_correlation_var shouldBe "?"
    e.communicationProperties.value.subject shouldBe "?"
    e.isAuto shouldBe false
  }

  test("transitionTestSendAnyWithAnyFromAny") {
    val e = parse(PASSParser.transition, """["foo" with correlation of "x" to * in "bar" /*|*/ with new correlation "y"] -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "foo"
    e.communicationProperties.value.with_correlation_var shouldBe "x"
    e.communicationProperties.value.subject shouldBe "*"
    e.communicationProperties.value.subjectVar shouldBe "bar"
    e.communicationProperties.value.new_correlation_var shouldBe "y"
    e.isAuto shouldBe false
  }

  test("transitionTestMessageToVarname") {
    val e = parse(PASSParser.transition, """["msg type" to A in "varname"] -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "A"
    e.communicationProperties.value.subjectVar shouldBe "varname"
    e.isAuto shouldBe false
  }

  test("transitionTestMessageFromVarname") {
    val e = parse(PASSParser.transition, """["msg type" from A in "varname"] -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "A"
    e.communicationProperties.value.subjectVar shouldBe "varname"
    e.isAuto shouldBe false
  }

  test("transitionTestMessageToMulti") {
    val e = parse(PASSParser.transition, """["msg type" to 3 of A] -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "A"
    e.communicationProperties.value.subjectCountMin shouldBe 3
    e.communicationProperties.value.subjectCountMax shouldBe 3
    e.isAuto shouldBe false
  }

  test("transitionTestMessageToMultiVarname") {
    val e = parse(PASSParser.transition, """["msg type" to 3 of A in "varname"] -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "A"
    e.communicationProperties.value.subjectCountMin shouldBe 3
    e.communicationProperties.value.subjectCountMax shouldBe 3
    e.communicationProperties.value.subjectVar shouldBe "varname"
    e.isAuto shouldBe false
  }

  test("transitionTestMessageToAnyVarname") {
    val e = parse(PASSParser.transition, """["msg type" to * in "varname"] -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "*"
    e.communicationProperties.value.subjectVar shouldBe "varname"
    e.isAuto shouldBe false
  }

  test("transitionTestMessageWithContent") {
    val e = parse(PASSParser.transition, """["msg type" to A with content of "varname"] -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "A"
    e.communicationProperties.value.content_var shouldBe "varname"
    e.isAuto shouldBe false
  }

  test("transitionTestMessageStoreContentFail") {
    an[ParserException] should be thrownBy {
      // `store content in` has been removed in favor of VarMan/ExtractContent
      parse(PASSParser.transition, """["msg type" from A store content in "varname"] -> nextState""")
    }
  }

  test("transitionTestMessageStoreChannelFail") {
    an[ParserException] should be thrownBy {
      // `store channel in` has been removed in favor of VarMan/ExtractChannel
      parse(PASSParser.transition, """["msg type" from A store channel in "varname"] -> nextState""")
    }
  }

  test("transitionTestMessageStoreCorrelationFail") {
    an[ParserException] should be thrownBy {
      // `store correlation in` has been removed in favor of VarMan/ExtractCorrelationID
      parse(PASSParser.transition, """["msg type" from A store correlation in "varname"] -> nextState""")
    }
  }

  test("transitionTestMessageStoreMessage") {
    val e = parse(PASSParser.transition, """["msg type" from A store message in "varname"] -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "A"
    e.communicationProperties.value.store_messages_var shouldBe "varname"
    e.isAuto shouldBe false
  }

  test("transitionTestMessageStoreReceiver") {
    val e = parse(PASSParser.transition, """["msg type" to A store receiver in "varname"] -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "A"
    e.communicationProperties.value.store_receiver_var shouldBe "varname"
    e.isAuto shouldBe false
  }

  /* // TODO: not implemented as this is not important
  test("transitionTestMessageStoreMessageMultiFail") {
    an[ParserException] should be thrownBy {
      // should fail as there are multiple messages to be stored
      parse(PASSParser.transition, "[\"msg type\" from 3 of A store message in \"varname\"] -> nextState");
    }
  }
  */

  test("transitionTestMessageStoreMessages") {
    val e = parse(PASSParser.transition, """["msg type" from 3 of A store messages in "varname"] -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "A"
    e.communicationProperties.value.subjectCountMin shouldBe 3
    e.communicationProperties.value.subjectCountMax shouldBe 3
    e.communicationProperties.value.store_messages_var shouldBe "varname"
    e.isAuto shouldBe false
  }

  /* // TODO: not implemented as this is not important
  test("transitionTestMessageStoreMessagesSingleFail") {
    an[ParserException] should be thrownBy {
      // should fail as there is only one messages to be stored
      parse(PASSParser.transition, "[\"msg type\" from A store messages in \"varname\"] -> nextState");
    }
  }
  */

  test("transitionTestMessageWithCorrelation") {
    val e = parse(PASSParser.transition, """["msg type" with correlation of "varname" to A] -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "A"
    e.communicationProperties.value.with_correlation_var shouldBe "varname"
    e.isAuto shouldBe false
  }

  test("transitionTestMessageWithNewCorrelation") {
    val e = parse(PASSParser.transition, """["msg type" to A with new correlation "varname"] -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "A"
    e.communicationProperties.value.new_correlation_var shouldBe "varname"
    e.isAuto shouldBe false
  }

  test("transitionTestMessageWithBothCorrelation") {
    val e = parse(PASSParser.transition, """["msg type" with correlation of "foo" to A with new correlation "bar"] -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "A"
    e.communicationProperties.value.with_correlation_var shouldBe "foo"
    e.communicationProperties.value.new_correlation_var shouldBe "bar"
    e.isAuto shouldBe false
  }

  test("transitionTestCorrelationDeprecatedFail") {
    an[ParserException] should be thrownBy {
      parse(PASSParser.transition, """["msg type" to A having correlation of "varname"] -> nextState""")
    }

    an[ParserException] should be thrownBy {
      parse(PASSParser.transition, """["msg type" to A with correlation of "varname"] -> nextState""")
    }
  }

  test("transitionTestCorrelationIllegalFail") {
    an[ParserException] should be thrownBy {
      parse(PASSParser.transition, """["msg type" with new correlation "varname" to A] -> nextState""")
    }

    an[ParserException] should be thrownBy {
      parse(PASSParser.transition, """["msg type" to A with new correlation ?] -> nextState""")
    }
  }

  // TODO: priority should be independent of Message
  test("transitionTestMessageWithPriority") {
    val e = parse(PASSParser.transition, """["msg type" to A] (with priority 2) -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "A"
    e.getPriority shouldBe 2
    e.isAuto shouldBe false
  }

  // TODO: priority should be independent of Message
  test("transitionTestMessageWithPriorityAuto") {
    val e = parse(PASSParser.transition, """["msg type" to A] (auto with priority 2) -> nextState""")

    e.label should not be 'defined
    e.targetStateID shouldBe "nextState"
    e.communicationProperties.value.msgType shouldBe "msg type"
    e.communicationProperties.value.subject shouldBe "A"
    e.getPriority shouldBe 2
    e.isAuto shouldBe true
  }


  test("transitionTestMessageCountMin") {
    val e = parse(PASSParser.transition, """["msg type" from min 1 max * of B] -> nextState""")

    e.communicationProperties.value.subjectCountMin shouldBe 1
    e.communicationProperties.value.subjectCountMax shouldBe 0
  }

  test("transitionTestMessageCountAll") {
    val e = parse(PASSParser.transition, """["msg type" from * of B in "foo"] -> nextState""")

    e.communicationProperties.value.subjectCountMin shouldBe 0
    e.communicationProperties.value.subjectCountMax shouldBe 0
    e.communicationProperties.value.subject shouldBe "B"
    e.communicationProperties.value.subjectVar shouldBe "foo"
  }

  test("transitionTestMessageCountMax") {
    val e = parse(PASSParser.transition, """["msg type" from min 1 max 2 of B] -> nextState""")

    e.communicationProperties.value.subjectCountMin shouldBe 1
    e.communicationProperties.value.subjectCountMax shouldBe 2
  }

  test("transitionTestMessageCountMinMax") {
    val e = parse(PASSParser.transition, """["msg type" from min 5 max 10 of B] -> nextState""")

    e.communicationProperties.value.subjectCountMin shouldBe 5
    e.communicationProperties.value.subjectCountMax shouldBe 10
  }

  test("transitionTestMessageCountMinMaxSend") {
    val e = parse(PASSParser.transition, """["msg type" to min 5 max 10 of B] -> nextState""")

    e.communicationProperties.value.subjectCountMin shouldBe 5
    e.communicationProperties.value.subjectCountMax shouldBe 10
  }
}
