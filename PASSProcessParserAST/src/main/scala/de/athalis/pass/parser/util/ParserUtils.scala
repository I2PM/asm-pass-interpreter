package de.athalis.pass.parser.util

import java.util.{List => JList}

import de.athalis.pass.parser.PASSParser
import de.athalis.pass.parser.ast._
import org.jparsec.{Parser, Parsers}

import scala.collection.JavaConverters._


object ScalaMapper {

  implicit class RichParser[A](private val self: Parser[A]) extends AnyVal {
    def asOption: Parser[Option[A]] = self.asOptional.map((x) => if (x.isPresent) Some(x.get) else None)
    def asSomeOption: Parser[Option[A]] = self.map[Option[A]]((x) => Some(x))
    def skip: Parser[Void] = self.skipTimes(1, 1)
  }

  implicit class RichJListParser[A](private val self: Parser[JList[A]]) extends AnyVal {
    def asSeq: Parser[Seq[A]] = self.map[Seq[A]](_.asScala)
    def asSet: Parser[Set[A]] = self.map[Set[A]](_.asScala.toSet)
  }

  implicit class RichJListMapAbleNodeParser[T](private val self: Parser[JList[MapAbleNode[T]]]) extends AnyVal {
    def asSeqNode: Parser[MapAbleNode[Seq[T]]] = self.map((from) => SEQNode.PARSER[T](from.asScala))
    def asSetNode: Parser[MapAbleNode[Set[T]]] = self.map((from) => SETNode.PARSER[T](from.asScala.toSet))
  }
}

object ParserUtils {

  def parsePASS[T](parser: Parser[T], source: String ): T = {
    val _parser: Parser[T] = parser.from(PASSParser.TOKENIZER, PASSParser.IGNORED)
    _parser.parse(source)
  }

  private def keepPair[A, B]: java.util.function.BiFunction[A, B, (A, B)] = (a: A, b: B) => (a, b)

  def parsePair[A, B](a: Parser[A], b: Parser[B]): Parser[(A, B)] = Parsers.sequence[A, B, (A, B)](a, b, keepPair[A, B])
}
