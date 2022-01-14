package de.athalis.pass.processmodel.parser.graphml.parser

import de.athalis.pass.processmodel.parser.ast.PASSParser
import de.athalis.pass.processmodel.parser.ast.PASSParser._
import de.athalis.pass.processmodel.parser.ast.node._
import de.athalis.pass.processmodel.parser.ast.node.pass._
import de.athalis.pass.processmodel.parser.ast.util.ParserUtils
import de.athalis.pass.processmodel.parser.ast.util.ScalaMapper._
import de.athalis.pass.processmodel.parser.graphml.Helper.ParserLocation

import org.jparsec.Parser
import org.jparsec.Parsers
import org.jparsec.error.ParserException

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object GraphMLJParser {
  val logger: Logger = LoggerFactory.getLogger(GraphMLJParser.getClass)

  val storeResultVarParser: Parser[MapAbleNode[String]] = op("=:").next(idOrStringParser)

  val storeReceiverVarParser: Parser[CommunicationTransitionProperty] = storeResultVarParser.map((from: MapAbleNode[String]) => {
    TransitionPropertyStoreReceiverIn(from.value)
  })

  val storeMessagesVarParser: Parser[CommunicationTransitionProperty] = storeResultVarParser.map((from: MapAbleNode[String]) => {
    TransitionPropertyStoreMessagesIn(from.value)
  })


  val subjectLabelParser: Parser[(MapAbleNode[String], MapAbleNode[Map[String, Any]])] = ParserUtils.parsePair(
    idOrStringParser,
    PASSParser.mapParser(idOrStringParser, mapAbleParser).optional(MAPNode.empty[String, Any])
  )


  val sendMsgEdgePropertiesParser: Parser[CommunicationTransitionNode] = Parsers.list(java.util.Arrays.asList(msgToSubj.asSomeOption, newCorrelation.atomic.asOption, withContentOf.asOption, storeReceiverVarParser.asOption)).asSeq.map(CommunicationTransitionNode.PARSEROptional)
  val receiveMsgEdgePropertiesParser: Parser[CommunicationTransitionNode] = Parsers.list(java.util.Arrays.asList(msgFromSubj.asSomeOption, storeMessagesVarParser.asOption)).asSeq.map(CommunicationTransitionNode.PARSEROptional)


  val idArgsParser: Parser[(MapAbleNode[String], Seq[MapAbleNode[Any]])] = ParserUtils.parsePair(idOrStringParser, PASSParser.stateArguments)
  val idOptionalStringArgsParser: Parser[(MapAbleNode[String], Seq[MapAbleNode[String]])] = ParserUtils.parsePair(idOrStringParser, PASSParser.stringArguments.optional(Seq.empty))
  val varMan: Parser[Seq[MapAbleNode[Any]]] = idArgsParser.map(VarManMapper)



  def VarManMapper: java.util.function.Function[(MapAbleNode[String], Seq[MapAbleNode[Any]]), Seq[MapAbleNode[Any]]] = (from: (MapAbleNode[String], Seq[MapAbleNode[Any]])) => {
    from._1 +: from._2
  }

  private def parse[T](parser: Parser[T], source: String)(implicit loc: ParserLocation): T = {
    logger.trace("parsing: {}", source)

    val _parser: Parser[T] = parser.from(TOKENIZER, IGNORED)
    try {
      _parser.parse(source)
    }
    catch {
      case e: ParserException => throw new IllegalArgumentException("unable to parse: '"+source+"' " + loc, e)
    }
  }

  def parseDataNode(label: String)(implicit loc: ParserLocation): DataNode = parse(dataParser, label)
  def parseSubjectLabel(label: String)(implicit loc: ParserLocation): (MapAbleNode[String], MapAbleNode[Map[String, Any]]) = parse(subjectLabelParser, label)
  def parseSendTransitionLabel(label: String)(implicit loc: ParserLocation): CommunicationTransitionNode = parse(sendMsgEdgePropertiesParser, label)
  def parseReceiveTransitionLabel(label: String)(implicit loc: ParserLocation): CommunicationTransitionNode = parse(receiveMsgEdgePropertiesParser, label)
  def parseVarManEdgeLabel(label: String)(implicit loc: ParserLocation): Seq[MapAbleNode[Any]] = parse(varMan, label)
  def parseSelectAgentsEdgeLabel(label: String)(implicit loc: ParserLocation): Seq[MapAbleNode[Any]] = parse(PASSParser.stateArguments, label)
  def parseIdArgsLabel(label: String)(implicit loc: ParserLocation): (MapAbleNode[String], Seq[MapAbleNode[Any]]) = parse(idArgsParser, label)
  def parseIdOptionalStringArgsLabel(label: String)(implicit loc: ParserLocation): (MapAbleNode[String], Seq[MapAbleNode[String]]) = parse(idOptionalStringArgsParser, label)

  def parseSubjectLabelRich(label: String)(implicit loc: ParserLocation): (String, Map[String, Any]) = {
    val x = parseSubjectLabel(label)
    (x._1.value, x._2.value)
  }

  val cleanQuotesParser: Parser[String] = idOrStringParser.map((from: MapAbleNode[String]) => {
    from.value
  })

  def cleanQuotes(x: String)(implicit loc: ParserLocation): String = parse(cleanQuotesParser, x)
}
