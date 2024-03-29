package de.athalis.pass.processmodel.parser.graphml.parser

import de.athalis.pass.processmodel.parser.ast.util.ParserUtils
import de.athalis.pass.processmodel.parser.graphml.Helper.ParserLocation

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class GraphMLJParserSpec extends AnyFunSuite with Matchers {
  import ParserUtils.parsePASSwithCause

  private implicit val loc: ParserLocation = ParserLocation("GraphMLJParserSpec", None)

  test("single receive edge") {
    val e = parsePASSwithCause(GraphMLJParser.receiveMsgEdgePropertiesParser, "Test from Foo")

    e.msgType shouldBe "Test"
    e.subject shouldBe "Foo"
  }

  test("single receive edge with subjVar") {
    val e = parsePASSwithCause(GraphMLJParser.receiveMsgEdgePropertiesParser, "Test from Foo in bar")

    e.subject shouldBe "Foo"
    e.subjectVar shouldBe "bar"
  }

  test("multi receive edge") {
    val e = parsePASSwithCause(GraphMLJParser.receiveMsgEdgePropertiesParser, "Test from 7 of Foo")

    e.subjectCountMin shouldBe 7
    e.subjectCountMax shouldBe 7
    e.subject shouldBe "Foo"
  }

  test("single receive edge with varname") {
    val e = parsePASSwithCause(GraphMLJParser.receiveMsgEdgePropertiesParser, "Test from Foo =: bar")

    e.store_messages_var shouldBe "bar"
  }



  test("send edge") {
    val e = parsePASSwithCause(GraphMLJParser.sendMsgEdgePropertiesParser, "Test to Foo")

    e.msgType shouldBe "Test"
    e.subject shouldBe "Foo"
  }

  test("send edge with subjectVar") {
    val e = parsePASSwithCause(GraphMLJParser.sendMsgEdgePropertiesParser, "Test to Foo in bar")

    e.subject shouldBe "Foo"
    e.subjectVar shouldBe "bar"
  }

  test("send edge with count") {
    val e = parsePASSwithCause(GraphMLJParser.sendMsgEdgePropertiesParser, "Test to 3 of Foo")

    e.subjectCountMin shouldBe 3
    e.subjectCountMax shouldBe 3
    e.subject shouldBe "Foo"
  }

  test("send edge store receiver") {
    val e = parsePASSwithCause(GraphMLJParser.sendMsgEdgePropertiesParser, "Test to Foo =: bar")

    e.msgType shouldBe "Test"
    e.store_receiver_var shouldBe "bar"
  }



  test("receive edge properties") {
    val e = parsePASSwithCause(GraphMLJParser.receiveMsgEdgePropertiesParser, "Test with correlation of \"corrID\" from 7 of Foo =: bar")

    e.msgType shouldBe "Test"
    e.with_correlation_var shouldBe "corrID"
    e.subject shouldBe "Foo"
    e.store_messages_var shouldBe "bar"
    e.subjectCountMax shouldBe 7
    e.subjectCountMin shouldBe 7
  }

  test("receive edge properties min/max") {
    val e = parsePASSwithCause(GraphMLJParser.receiveMsgEdgePropertiesParser, "Test from min 7 max 10 of Foo")

    e.subjectCountMin shouldBe 7
    e.subjectCountMax shouldBe 10
  }

  test("send edge properties correlation") {
    val e = parsePASSwithCause(GraphMLJParser.sendMsgEdgePropertiesParser, "Test with correlation of \"A\" to Foo with new correlation \"B\"")

    e.msgType shouldBe "Test"
    e.subject shouldBe "Foo"
    e.with_correlation_var shouldBe "A"
    e.new_correlation_var shouldBe "B"
  }



  test("varman edge") {
    val e = parsePASSwithCause(GraphMLJParser.varMan, "asdf(foo, bar, \"foo bar\", 1, [1, 2], {3, 4}, {a -> b})")

    e.map(_.value) shouldBe Seq("asdf", "foo", "bar", "foo bar", 1, Seq(1, 2), Set(3, 4), Map("a" -> "b"))
  }

  test("subject attributes simple") {
    val (label, args) = GraphMLJParser.parseSubjectLabelRich("A")

    label shouldBe "A"
    args shouldBe Map.empty
  }

  test("subject attributes complex") {
    val (label, args) = GraphMLJParser.parseSubjectLabelRich("A{a -> 1, b -> [5, 6], c -> {a, b}}")

    label shouldBe "A"
    args shouldBe Map("a" -> 1, "b" -> Seq(5, 6), "c" -> Set("a", "b"))
  }
}
