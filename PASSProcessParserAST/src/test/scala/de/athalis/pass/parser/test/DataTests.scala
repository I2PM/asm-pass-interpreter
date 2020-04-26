package de.athalis.pass.parser.test

import org.jparsec.error.ParserException
import org.scalatest.{FunSuite, Matchers}

import de.athalis.pass.parser.PASSParser

class DataTests extends FunSuite with Matchers {
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
