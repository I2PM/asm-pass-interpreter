package de.athalis.pass.processmodel.parser.ast.test

import de.athalis.pass.processmodel.parser.ast.PASSParser
import de.athalis.pass.processmodel.parser.ast.util.ParserUtils

import org.jparsec.error.ParserException

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class DataTests extends AnyFunSuite with Matchers {
  import ParserUtils.parsePASSwithCause

  test("dataTestFail1") {
    an[ParserException] should be thrownBy {
      // must be a Map on top-level
      parsePASSwithCause(PASSParser.dataParser, """Data { "foo1" }""")
    }
  }

  test("data parsing") {
    parsePASSwithCause(PASSParser.dataParser, """Data { "foo1" ->   "bar"   }""")
    parsePASSwithCause(PASSParser.dataParser, """Data { "foo1" ->    bar    }""")
    parsePASSwithCause(PASSParser.dataParser, """Data { "foo1" ->    5      }""")
    parsePASSwithCause(PASSParser.dataParser, """Data { "foo1" ->  ["bar"]  }""")
    parsePASSwithCause(PASSParser.dataParser, """Data { "foo1" ->  {"bar"}  }""")
    parsePASSwithCause(PASSParser.dataParser, """Data { "foo1" -> [["bar"]] }""")
    parsePASSwithCause(PASSParser.dataParser, """Data { "foo1" ->  ["bar1", ["bar2", ["bar3"], "bar4"]] }""")
    parsePASSwithCause(PASSParser.dataParser, """Data { "foo1" ->  {} }""")
    parsePASSwithCause(PASSParser.dataParser, """Data { "foo1" ->  {->} }""")
    parsePASSwithCause(PASSParser.dataParser, """Data { "foo1" ->  {"bar1" -> "bar2"} }""")
    parsePASSwithCause(PASSParser.dataParser, """Data { "foo1" ->  {"bar2" -> "bar3", "bar4" -> "bar5"} }""")
  }
}
