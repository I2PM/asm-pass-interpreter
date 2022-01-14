package de.athalis.pass.processmodel.parser.ast.test

import de.athalis.pass.processmodel.parser.ast.PASSParser

import org.jparsec.error.ParserException

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class DataTests extends AnyFunSuite with Matchers {
  import Util._

  test("dataTestFail1") {
    an[ParserException] should be thrownBy {
      // must be a Map on top-level
      parse(PASSParser.dataParser, """Data { "foo1" }""")
    }
  }

  test("data parsing") {
    parse(PASSParser.dataParser, """Data { "foo1" ->   "bar"   }""")
    parse(PASSParser.dataParser, """Data { "foo1" ->    bar    }""")
    parse(PASSParser.dataParser, """Data { "foo1" ->    5      }""")
    parse(PASSParser.dataParser, """Data { "foo1" ->  ["bar"]  }""")
    parse(PASSParser.dataParser, """Data { "foo1" ->  {"bar"}  }""")
    parse(PASSParser.dataParser, """Data { "foo1" -> [["bar"]] }""")
    parse(PASSParser.dataParser, """Data { "foo1" ->  ["bar1", ["bar2", ["bar3"], "bar4"]] }""")
    parse(PASSParser.dataParser, """Data { "foo1" ->  {} }""")
    parse(PASSParser.dataParser, """Data { "foo1" ->  {->} }""")
    parse(PASSParser.dataParser, """Data { "foo1" ->  {"bar1" -> "bar2"} }""")
    parse(PASSParser.dataParser, """Data { "foo1" ->  {"bar2" -> "bar3", "bar4" -> "bar5"} }""")
  }
}
