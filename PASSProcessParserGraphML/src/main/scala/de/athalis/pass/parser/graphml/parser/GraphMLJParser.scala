package de.athalis.pass.parser.graphml.parser

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.jparsec.{Parser, Parsers}
import org.jparsec.error.ParserException

import de.athalis.pass.parser.util.ParserUtils

import de.athalis.pass.parser.ast._
import de.athalis.pass.parser.ast.pass._
import de.athalis.pass.parser.PASSParser
import de.athalis.pass.parser.PASSParser._

import de.athalis.pass.parser.util.ScalaMapper._

object GraphMLJParser {
  val logger: Logger = LoggerFactory.getLogger(GraphMLJParser.getClass)

  val storeResultVarParser: Parser[MapAbleNode[String]] = op("=:").next(idOrStringParser)

  val storeReceiverVarParser: Parser[CommunicationTransitionProperty] = storeResultVarParser.map((from: MapAbleNode[String]) => {
    TransitionPropertyStoreReceiverIn(from.value)
  })

  val storeMessagesVarParser: Parser[CommunicationTransitionProperty] = storeResultVarParser.map((from: MapAbleNode[String]) => {
    TransitionPropertyStoreMessagesIn(from.value)
  })


  val subjectLabelParser: Parser[(MapAbleNode[String], MapAbleNode[Map[Any, Any]])] = ParserUtils.parsePair(idOrStringParser, PASSParser.mapParser.optional(MAPNode.empty))


  val sendMsgEdgePropertiesParser: Parser[CommunicationTransitionNode] = Parsers.list(java.util.Arrays.asList(msgToSubj.asSomeOption, newCorrelation.atomic.asOption, withContentOf.asOption, storeReceiverVarParser.asOption)).asSeq.map(CommunicationTransitionNode.PARSEROptional)
  val receiveMsgEdgePropertiesParser: Parser[CommunicationTransitionNode] = Parsers.list(java.util.Arrays.asList(msgFromSubj.asSomeOption, storeMessagesVarParser.asOption)).asSeq.map(CommunicationTransitionNode.PARSEROptional)


  val idArgsParser: Parser[(MapAbleNode[String], Seq[MapAbleNode[Any]])] = ParserUtils.parsePair(idOrStringParser, PASSParser.stateArguments)
  val idOptionalStringArgsParser: Parser[(MapAbleNode[String], Seq[MapAbleNode[String]])] = ParserUtils.parsePair(idOrStringParser, PASSParser.stringArguments.optional(Seq.empty))
  val varMan: Parser[Seq[MapAbleNode[Any]]] = idArgsParser.map(VarManMapper)



  def VarManMapper: java.util.function.Function[(MapAbleNode[String], Seq[MapAbleNode[Any]]), Seq[MapAbleNode[Any]]] = (from: (MapAbleNode[String], Seq[MapAbleNode[Any]])) => {
    from._1 +: from._2
  }

  private def parse[T](parser: Parser[T], source: String): T = {
    logger.trace("parsing: {}", source)

    val _parser: Parser[T] = parser.from(TOKENIZER, IGNORED)
    try {
      _parser.parse(source)
    }
    catch {
      case e: ParserException => throw new IllegalArgumentException("invalid source: '"+source+"'", e)
    }
  }

  def parseDataNode(label: String): DataNode = parse(dataParser, label)
  def parseSubjectLabel(label: String): (MapAbleNode[String], MapAbleNode[Map[Any, Any]]) = parse(subjectLabelParser, label)
  def parseSendTransitionLabel(label: String): CommunicationTransitionNode = parse(sendMsgEdgePropertiesParser, label)
  def parseReceiveTransitionLabel(label: String): CommunicationTransitionNode = parse(receiveMsgEdgePropertiesParser, label)
  def parseVarManEdgeLabel(label: String): Seq[MapAbleNode[Any]] = parse(varMan, label)
  def parseSelectAgentsEdgeLabel(label: String): Seq[MapAbleNode[Any]] = parse(PASSParser.stateArguments, label)
  def parseIdArgsLabel(label: String): (MapAbleNode[String], Seq[MapAbleNode[Any]]) = parse(idArgsParser, label)
  def parseIdOptionalStringArgsLabel(label: String): (MapAbleNode[String], Seq[MapAbleNode[String]]) = parse(idOptionalStringArgsParser, label)

  def parseSubjectLabelRich(label: String): (String, Map[Any, Any]) = {
    val x = parseSubjectLabel(label)
    (x._1.value, x._2.value)
  }

  val cleanQuotesParser: Parser[String] = idOrStringParser.map((from: MapAbleNode[String]) => {
    from.value
  })

  def cleanQuotes(x: String): String = parse(cleanQuotesParser, x)
}
