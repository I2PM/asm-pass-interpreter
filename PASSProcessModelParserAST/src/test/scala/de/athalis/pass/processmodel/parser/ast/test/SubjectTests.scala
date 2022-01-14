package de.athalis.pass.processmodel.parser.ast.test

import de.athalis.pass.processmodel.parser.ast.PASSParser

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SubjectTests extends AnyFunSuite with Matchers {
  import Util._

  test("subjectTestSimple") {
    val s = parse(PASSParser.subjectParser, """Subject Test { }""")

    s.id shouldBe "Test"
    s.getMacros shouldBe empty
    s.getInputPoolSize shouldBe 100
    s.isStartSubject shouldBe false
  }

  test("subjectTestSimpleNamed") {
    val s = parse(PASSParser.subjectParser, """Subject "A B C" { }""")

    s.id shouldBe "A B C"
    s.getMacros shouldBe empty
  }

  test("externalSubjectTestSimple") {
    val s = parse(PASSParser.interfaceSubjectParser, """InterfaceSubject Foo is Bar in Baz""")

    s.id shouldBe "Foo"
    s.externalSubjectID shouldBe "Bar"
    s.externalProcessID shouldBe "Baz"
    s.getMacros shouldBe empty
  }

  test("externalSubjectTestSimpleNamed") {
    val s = parse(PASSParser.interfaceSubjectParser, """InterfaceSubject "A B C" is "D E F" in "X Y Z"""")

    s.id shouldBe "A B C"
    s.externalSubjectID shouldBe "D E F"
    s.externalProcessID shouldBe "X Y Z"
    s.getMacros shouldBe empty
  }

  test("externalSubjectTestSimpleAnonymous") {
    val s = parse(PASSParser.interfaceSubjectParser, """InterfaceSubject Foo is ? in ?""")

    s.id shouldBe "Foo"
    s.externalSubjectID shouldBe "?"
    s.externalProcessID shouldBe "?"
    s.getMacros shouldBe empty
  }

  test("subjectTest IP Size") {
    val s = parse(PASSParser.subjectParser, """Subject Test { InputPool := 2 }""")

    s.id shouldBe "Test"
    s.getMacros shouldBe empty
    s.getInputPoolSize shouldBe 2
  }

  test("subjectTest StartSubject") {
    val s = parse(PASSParser.subjectParser, """Subject Test { StartSubject := true }""")

    s.id shouldBe "Test"
    s.getMacros shouldBe empty
    s.isStartSubject shouldBe true
  }

  test("subjectTest Macro") {
    val s = parse(PASSParser.subjectParser, """Subject Test { Macro Foo {} }""")

    s.id shouldBe "Test"
    s.getMacros should have size 1
  }
}
