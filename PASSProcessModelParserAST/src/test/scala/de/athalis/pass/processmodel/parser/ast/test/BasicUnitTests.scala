package de.athalis.pass.processmodel.parser.ast.test

import de.athalis.pass.processmodel.parser.ast.PASSParser
import de.athalis.pass.processmodel.parser.ast.node._
import de.athalis.pass.processmodel.parser.ast.util.ParserUtils

import org.jparsec.error.ParserException

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class BasicUnitTests extends AnyFunSuite with Matchers {
  import ParserUtils.parsePASSwithCause

  test("idTest") {
    parsePASSwithCause(PASSParser.ID, "foo") shouldBe new IDNode("foo")
  }

  test("oprTest") {
    parsePASSwithCause(PASSParser.allParser, "*") shouldBe new OPRNode("*")
  }

  test("kwTest") {
    parsePASSwithCause(PASSParser.kw("true"), "true") shouldBe new KWNode("true")
  }

  test("stringTest") {
    parsePASSwithCause(PASSParser.STRING, "\"Hallo Welt\"") shouldBe new STRINGNode("Hallo Welt")
  }

  test("numberTest") {
    parsePASSwithCause(PASSParser.NUMBER, "123") shouldBe new NUMBERNode(123)
  }



  test("idOrStringTestID") {
    parsePASSwithCause(PASSParser.idOrStringParser, "a") shouldBe new IDNode("a")
  }

  test("idOrStringTestString") {
    parsePASSwithCause(PASSParser.idOrStringParser, "\"a b\"") shouldBe new STRINGNode("a b")
  }



  test("idOrStringOrAnyTestID") {
    parsePASSwithCause(PASSParser.idOrStringOrAnyParser, "a") shouldBe new IDNode("a")
  }

  test("idOrStringOrAnyTestString") {
    parsePASSwithCause(PASSParser.idOrStringOrAnyParser, "\"a b\"") shouldBe new STRINGNode("a b")
  }

  test("idOrStringOrAnyTestAny") {
    parsePASSwithCause(PASSParser.idOrStringOrAnyParser, "?") shouldBe new OPRNode("?")
  }

  test("idOrStringOrAnyTestAll") {
    parsePASSwithCause(PASSParser.idOrStringOrAllParser, "*") shouldBe new OPRNode("*")
  }

  test("idOrStringOrAnyTestAnyOrAll") {
    parsePASSwithCause(PASSParser.idOrStringOrAnyOrAllParser, "?") shouldBe new OPRNode("?")
    parsePASSwithCause(PASSParser.idOrStringOrAnyOrAllParser, "*") shouldBe new OPRNode("*")
  }



  test("idOrStringOrNrTestID") {
    parsePASSwithCause(PASSParser.idOrStringOrNrParser, "a") shouldBe new IDNode("a")
  }

  test("idOrStringOrNrTestString") {
    parsePASSwithCause(PASSParser.idOrStringOrNrParser, "\"a b\"") shouldBe new STRINGNode("a b")
  }

  test("idOrStringOrNrTestAny") {
    parsePASSwithCause(PASSParser.idOrStringOrNrParser, "123") shouldBe new NUMBERNode(123)
  }



  test("paramsTest1") {
    parsePASSwithCause(PASSParser.params, "a := b")     shouldBe ((new IDNode("a"), new IDNode("b")))
    parsePASSwithCause(PASSParser.params, "a := \"b\"") shouldBe ((new IDNode("a"), new STRINGNode("b")))
    parsePASSwithCause(PASSParser.params, "a := 12")    shouldBe ((new IDNode("a"), new NUMBERNode(12)))
  }

  test("paramsTestList") {
    parsePASSwithCause(PASSParser.params, "a := []")       shouldBe ((new IDNode("a"), new SEQNode[Nothing](Seq())))
    parsePASSwithCause(PASSParser.params, "a := [12, 13]") shouldBe ((new IDNode("a"), new SEQNode[Int](Seq(12, 13))))
  }

  test("paramsTestListInvalid") {
    an[ParserException] shouldBe thrownBy {
      parsePASSwithCause(PASSParser.params, "a := [12 13]")
    }
  }

  test("paramsTestSet") {
    parsePASSwithCause(PASSParser.params, "a := {}")       shouldBe ((new IDNode("a"), new SETNode[Nothing](Set())))
    parsePASSwithCause(PASSParser.params, "a := {12, 13}") shouldBe ((new IDNode("a"), new SETNode[Int](Set(12, 13))))
  }

  test("paramsTestSetInvalid") {
    an[ParserException] shouldBe thrownBy {
      parsePASSwithCause(PASSParser.params, "a := {12 13}")
    }
  }

  test("mapTest") {
    parsePASSwithCause(PASSParser.mapParserAny, "{->}")                        shouldBe (MAPNode.empty)
    parsePASSwithCause(PASSParser.mapParserAny, "{a -> b}")                    shouldBe (MAPNode(Map("a" -> "b")))
    parsePASSwithCause(PASSParser.mapParserAny, "{\"a\" -> \"b\"}")            shouldBe (MAPNode(Map("a" -> "b")))
    parsePASSwithCause(PASSParser.mapParserAny, "{a -> b, c -> d}")            shouldBe (MAPNode(Map("a" -> "b", "c" -> "d")))
    parsePASSwithCause(PASSParser.mapParserAny, "{1 -> [1,2], x -> {a -> b}}") shouldBe (MAPNode(Map(1 -> Vector(1,2), "x" -> Map("a" -> "b"))))
    parsePASSwithCause(PASSParser.mapParser(PASSParser.ID, PASSParser.ID), "{a -> b}") shouldBe (MAPNode(Map("a" -> "b")))
    parsePASSwithCause(PASSParser.mapParser(PASSParser.NUMBER, PASSParser.ID), "{1 -> b}") shouldBe (MAPNode(Map(1 -> "b")))
    parsePASSwithCause(PASSParser.mapParser(PASSParser.ID, PASSParser.listParser(PASSParser.NUMBER)), "{a -> [1,2]}") shouldBe (MAPNode(Map("a" -> Seq(1,2))))
    parsePASSwithCause(PASSParser.mapParser(PASSParser.NUMBER, PASSParser.ID), "{1 -> b}") shouldBe (MAPNode(Map(1 -> "b")))
  }

  test("mapTestInvalidKey") {
    an[ParserException] shouldBe thrownBy {
      parsePASSwithCause(PASSParser.mapParser(PASSParser.idOrStringParser, PASSParser.mapAbleParser), "{1 -> b}")
    }
  }

  test("mapTestInvalidValue") {
    an[ParserException] shouldBe thrownBy {
      parsePASSwithCause(PASSParser.mapParser(PASSParser.mapAbleParser, PASSParser.idOrStringParser), "{a -> [1,2]}")
    }
  }
}
