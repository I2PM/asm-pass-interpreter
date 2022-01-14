package de.athalis.pass.processmodel.parser.ast.test

import de.athalis.pass.processmodel.parser.ast.PASSParser

import org.jparsec.Parser
import org.jparsec.error.ParserException

object Util {
  def parse[T](parser: Parser[T], source: String): T = {
    val _parser: Parser[T] = parser.from(PASSParser.TOKENIZER, PASSParser.IGNORED)
    try {
      _parser.parse(source)
    }
    catch {
      case e: ParserException if e.getCause != null => throw e.getCause
      case x: Throwable => throw x
    }
  }
}
