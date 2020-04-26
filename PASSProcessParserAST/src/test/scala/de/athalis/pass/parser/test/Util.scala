package de.athalis.pass.parser.test

import org.jparsec.Parser
import org.jparsec.error.ParserException

import de.athalis.pass.parser.PASSParser

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
