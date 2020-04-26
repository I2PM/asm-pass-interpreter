package de.athalis.pass.parser.test

import org.jparsec.error.ParserException
import org.scalatest.{FunSuite, Matchers}

import de.athalis.pass.parser.PASSParser
import de.athalis.pass.parser.ast._

class BasicUnitTests extends FunSuite with Matchers {
  import Util._

  test("idTest") {
    parse(PASSParser.ID, "foo") shouldBe new IDNode("foo")
  }

  test("oprTest") {
    parse(PASSParser.allParser, "*") shouldBe new OPRNode("*")
  }

  test("kwTest") {
    parse(PASSParser.kw("true"), "true") shouldBe new KWNode("true")
  }

  test("stringTest") {
    parse(PASSParser.STRING, "\"Hallo Welt\"") shouldBe new STRINGNode("Hallo Welt")
  }

  test("numberTest") {
    parse(PASSParser.NUMBER, "123") shouldBe new NUMBERNode(123)
  }



  test("idOrStringTestID") {
    parse(PASSParser.idOrStringParser, "a") shouldBe new IDNode("a")
  }

  test("idOrStringTestString") {
    parse(PASSParser.idOrStringParser, "\"a b\"") shouldBe new STRINGNode("a b")
  }



  test("idOrStringOrAnyTestID") {
    parse(PASSParser.idOrStringOrAnyParser, "a") shouldBe new IDNode("a")
  }

  test("idOrStringOrAnyTestString") {
    parse(PASSParser.idOrStringOrAnyParser, "\"a b\"") shouldBe new STRINGNode("a b")
  }

  test("idOrStringOrAnyTestAny") {
    parse(PASSParser.idOrStringOrAnyParser, "?") shouldBe new OPRNode("?")
  }

  test("idOrStringOrAnyTestAll") {
    parse(PASSParser.idOrStringOrAllParser, "*") shouldBe new OPRNode("*")
  }

  test("idOrStringOrAnyTestAnyOrAll") {
    parse(PASSParser.idOrStringOrAnyOrAllParser, "?") shouldBe new OPRNode("?")
    parse(PASSParser.idOrStringOrAnyOrAllParser, "*") shouldBe new OPRNode("*")
  }



  test("idOrStringOrNrTestID") {
    parse(PASSParser.idOrStringOrNrParser, "a") shouldBe new IDNode("a")
  }

  test("idOrStringOrNrTestString") {
    parse(PASSParser.idOrStringOrNrParser, "\"a b\"") shouldBe new STRINGNode("a b")
  }

  test("idOrStringOrNrTestAny") {
    parse(PASSParser.idOrStringOrNrParser, "123") shouldBe new NUMBERNode(123)
  }



  test("paramsTest1") {
    parse(PASSParser.params, "a := b")     shouldBe ((new IDNode("a"), new IDNode("b")))
    parse(PASSParser.params, "a := \"b\"") shouldBe ((new IDNode("a"), new STRINGNode("b")))
    parse(PASSParser.params, "a := 12")    shouldBe ((new IDNode("a"), new NUMBERNode(12)))
  }

  test("paramsTestList") {
    parse(PASSParser.params, "a := []")       shouldBe ((new IDNode("a"), new SEQNode[Nothing](Seq())))
    parse(PASSParser.params, "a := [12, 13]") shouldBe ((new IDNode("a"), new SEQNode[Int](Seq(12, 13))))
  }

  test("paramsTestListInvalid") {
    an[ParserException] shouldBe thrownBy {
      parse(PASSParser.params, "a := [12 13]")
    }
  }

  test("paramsTestSet") {
    parse(PASSParser.params, "a := {}")       shouldBe ((new IDNode("a"), new SETNode[Nothing](Set())))
    parse(PASSParser.params, "a := {12, 13}") shouldBe ((new IDNode("a"), new SETNode[Int](Set(12, 13))))
  }

  test("paramsTestSetInvalid") {
    an[ParserException] shouldBe thrownBy {
      parse(PASSParser.params, "a := {12 13}")
    }
  }
}
