package de.athalis.pass.parser

import org.jparsec.Parser.Reference
import org.jparsec.{Parser, Parsers, Scanners, Terminals}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import de.athalis.pass.parser.ast._
import de.athalis.pass.parser.ast.pass._
import de.athalis.pass.parser.util.ScalaMapper._
import de.athalis.pass.parser.util._

object PASSParser {
  val logger: Logger = LoggerFactory.getLogger(PASSParser.getClass)

  val KW_PROCESS: String           = "Process"
  val KW_DATA: String              = "Data"
  val KW_SUBJECT: String           = "Subject"
  val KW_INTERFACE_SUBJECT: String = "InterfaceSubject"
  val KW_MACRO: String             = "Macro"

  val keywordsProcess: Set[String] = Set(
    KW_PROCESS,
    KW_DATA,
    KW_SUBJECT,
    KW_INTERFACE_SUBJECT,
    KW_MACRO)



  val KW_AUTO: String           = "auto"
  val KW_HIDDEN: String         = "hidden"

  val KW_TIMEOUT: String        = "timeout"
  val KW_CANCEL: String         = "cancel"

  val KW_SET: String            = "set"
  val KW_UNION: String          = "union"
  val KW_INTERSECT: String      = "intersect"
  val KW_COMPLEMENT: String     = "complement"
  val KW_SELECT: String         = "select"
  val KW_EXTRACT: String        = "extract"

  val KW_FROM: String           = "from"
  val KW_TO: String             = "to"

  val KW_MIN: String            = "min"
  val KW_MAX: String            = "max"

  val KW_IS: String             = "is"
  val KW_IN: String             = "in"
  val KW_OF: String             = "of"
  val KW_WITH: String           = "with"
  val KW_NEW: String            = "new"
  val KW_STORE: String          = "store"

  val KW_PRIORITY: String       = "priority"

  val KW_CONTENT: String        = "content"
  val KW_CORRELATION: String    = "correlation"
  val KW_MESSAGE: String        = "message"
  val KW_MESSAGES: String       = "messages"
  val KW_RECEIVER: String       = "receiver"

  val OPR_ANY: String           = "?"
  val OPR_ALL: String           = "*"

  val keywordsTransitionProperties: Set[String] = Set(

      KW_AUTO,
      KW_HIDDEN,
      KW_TIMEOUT,
      KW_CANCEL,

      KW_SET,
      KW_UNION,
      KW_INTERSECT,
      KW_COMPLEMENT,
      KW_SELECT,
      KW_EXTRACT,

      KW_FROM,
      KW_TO,

      KW_MIN,
      KW_MAX,

      KW_IS,
      KW_IN,
      KW_OF,
      KW_WITH,
      KW_NEW,
      KW_STORE,

      KW_PRIORITY,

      KW_CONTENT,
      KW_CORRELATION,
      KW_MESSAGE,
      KW_MESSAGES,
      KW_RECEIVER)

  val keywordsList: Set[String] = keywordsProcess ++ keywordsTransitionProperties ++ Set("true", "false")

  val operators: Set[String] = Set(
    "=:", ":=", ":", "->", "=>",
    ",", "(", ")", "{", "}", "[", "]",
    OPR_ANY, OPR_ALL
  )

  val terms: Terminals = Terminals
    .operators(operators.asJava)
    .words(Scanners.IDENTIFIER)
    .keywords(keywordsList.asJava)
    .build()

  def op(name: String): Parser[MapAbleNode[String]] = op(Seq(name): _*)

  def op(names: String*): Parser[MapAbleNode[String]] = terms.token(names: _*).map(OPRNode.PARSER)

  def kw(name: String): Parser[MapAbleNode[String]] = kw(Seq(name): _*)

  def kw(names: String*): Parser[MapAbleNode[String]] = terms.token(names: _*).map(KWNode.PARSER)

  val TOKENIZER: Parser[_] = Parsers.or(Seq(
    Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER,
    Terminals.StringLiteral.DOUBLE_QUOTE_TOKENIZER,
    Terminals.IntegerLiteral.TOKENIZER,
    terms.tokenizer().cast()
  ): _*)

  // custom parameters

  val IGNORED: Parser[Void] = Parsers.or(Seq(Scanners.JAVA_LINE_COMMENT, Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES): _*).skipMany()

  val ID: Parser[MapAbleNode[String]] = Terminals.Identifier.PARSER.token().map(IDNode.PARSER)
  val NUMBER: Parser[MapAbleNode[Int]] = Terminals.IntegerLiteral.PARSER.map(NUMBERNode.PARSER)
  val STRING: Parser[MapAbleNode[String]] = Terminals.StringLiteral.PARSER.map(STRINGNode.PARSER)

  val BOOLEAN: Parser[MapAbleNode[Boolean]] = Parsers.or(Seq(
    kw("true"),
    kw("false")
  ): _*).map(BOOLEANNode.PARSER)

  val anyParser: Parser[MapAbleNode[String]] = op(OPR_ANY)
  val allParser: Parser[MapAbleNode[String]] = op(OPR_ALL)

  val comma: Parser[MapAbleNode[String]] = op(",")

  val lParen: Parser[MapAbleNode[String]] = op("(")
  val rParen: Parser[MapAbleNode[String]] = op(")")
  val lParenSqr: Parser[MapAbleNode[String]] = op("[")
  val rParenSqr: Parser[MapAbleNode[String]] = op("]")
  val lParenCurl: Parser[MapAbleNode[String]] = op("{")
  val rParenCurl: Parser[MapAbleNode[String]] = op("}")

  val allParserInt: Parser[MapAbleNode[Int]] = allParser.map[MapAbleNode[Int]](OPRNodeInt.PARSER)

  val numberOrAllParser: Parser[MapAbleNode[Int]] = Parsers.or(Seq(NUMBER, allParserInt): _*)

  val idOrStringParser: Parser[MapAbleNode[String]] = Parsers.or(Seq(ID, STRING): _*)
  val idOrStringOrAnyParser: Parser[MapAbleNode[String]] = Parsers.or(Seq(ID, STRING, anyParser): _*)
  val idOrStringOrAllParser: Parser[MapAbleNode[String]] = Parsers.or(Seq(ID, STRING, allParser): _*)
  val idOrStringOrAnyOrAllParser: Parser[MapAbleNode[String]] = Parsers.or(Seq(ID, STRING, anyParser, allParser): _*)
  val idOrStringOrNrParser: Parser[MapAbleNode[Any]] = Parsers.or(Seq(ID, STRING, NUMBER): _*)
  val idOrStringOrNrOrAnyOrAllParser: Parser[MapAbleNode[Any]] = Parsers.or(Seq(ID, STRING, NUMBER, anyParser, allParser): _*)


  private val MapAbleRef: Reference[MapAbleNode[Any]] = Parser.newReference()


  val listParser: Parser[MapAbleNode[Seq[Any]]] = MapAbleRef.`lazy`().sepBy(comma).asSeqNode.between(lParenSqr, rParenSqr)
  val setParser: Parser[MapAbleNode[Set[Any]]] = MapAbleRef.`lazy`().sepBy(comma).asSetNode.between(lParenCurl, rParenCurl)

  val mapParser: Parser[MapAbleNode[Map[Any, Any]]] = Parsers.or(Seq(
    kw("->").between(lParenCurl, rParenCurl).map[MapAbleNode[Map[Any, Nothing]]](MAPNode.PARSEREmpty), {
      val a: Parser[(MapAbleNode[Any], MapAbleNode[Any])] = ParserUtils.parsePair(MapAbleRef.`lazy`().followedBy(kw("->")), MapAbleRef.`lazy`())
      val b: Parser[MapAbleNode[Map[Any, Any]]] = a.sepBy(comma).asSeq.between(lParenCurl, rParenCurl).map(MAPNode.PARSERAny)
      b
    }
  ): _*)

  private val MapAbleParser: Parser[MapAbleNode[Any]] =
    Parsers.or(Seq(
      BOOLEAN,
      idOrStringOrNrOrAnyOrAllParser,
      listParser,
      setParser.atomic,
      mapParser
    ): _*)

  MapAbleRef.set(MapAbleParser)


  val dataParser: Parser[DataNode] = kw(KW_DATA).next(mapParser).map(DataNode.PARSER)


  val params: Parser[(MapAbleNode[String], MapAbleNode[_])] = ParserUtils.parsePair(
    ID.followedBy(op(":=")).atomic, // atomic falls back instead of failing, this is needed as e.g. states start also with ID
    MapAbleParser
  )


  private val transitionsPropertyParsers: Set[Parser[TransitionProperty]] = Set(
    kw(PASSParser.KW_AUTO) map ((_) => TransitionPropertyAuto),

    kw(PASSParser.KW_HIDDEN) map ((_) => TransitionPropertyHidden),

    kw(PASSParser.KW_TIMEOUT) next NUMBER.between(PASSParser.lParen, PASSParser.rParen) map ((from: MapAbleNode[Int]) => TransitionPropertyTimeout(from.value)),

    kw(PASSParser.KW_CANCEL) map ((_) => TransitionPropertyCancel),

    kw(PASSParser.KW_WITH) next kw(PASSParser.KW_PRIORITY) next NUMBER map ((from: MapAbleNode[Int]) => TransitionPropertyPriority(from.value))
  )

  private val msgTypeWithCorrelationParser: Parser[TransitionPropertyMsgTypeWithCorrelation] = Parsers.sequence(
    idOrStringOrAnyParser,
    (kw(PASSParser.KW_WITH) next kw(PASSParser.KW_CORRELATION) next kw(PASSParser.KW_OF) next idOrStringOrAnyParser).asOption,

    (msgType: MapAbleNode[String], withCorrelation: Option[MapAbleNode[String]]) => {
      TransitionPropertyMsgTypeWithCorrelation(msgType.value, withCorrelation.map(_.value))
    }
  )


  val countMinMaxParserAll: Parser[CountMinMax] = allParser.skip map ((_) => {
    CountMinMax(None, None)
  })

  val countMinMaxParserExact: Parser[CountMinMax] = NUMBER map ((minMax: MapAbleNode[Int]) => {
    CountMinMax(Some(minMax.value), Some(minMax.value))
  })

  val countMinMaxParserMinMax: Parser[CountMinMax] = Parsers.sequence(
    kw("min") next NUMBER,
    kw("max") next NUMBER,

    (min: MapAbleNode[Int], max: MapAbleNode[Int]) => {
      CountMinMax(Some(min.value), Some(max.value))
    }
  )

  val countMinMaxParserMinNoLimit: Parser[CountMinMax] =
    kw("min") next NUMBER followedBy kw("max") followedBy allParser map ((min: MapAbleNode[Int]) => {
      CountMinMax(Some(min.value), None)
    })

  val countMinMaxParser: Parser[CountMinMax] = Parsers.or(Seq(countMinMaxParserAll, countMinMaxParserExact, countMinMaxParserMinMax.atomic(), countMinMaxParserMinNoLimit): _*)

  val msgFromSubj: Parser[CommunicationTransitionProperty] = Parsers.sequence(
    msgTypeWithCorrelationParser,
    kw(PASSParser.KW_FROM).skip,
    countMinMaxParser.followedBy(kw(PASSParser.KW_OF)).asOption, // subject count
    idOrStringOrAnyParser, // subjectID
    (kw(PASSParser.KW_IN) next idOrStringParser).asOption, // channel

    (msgTypeWithCorrelation: TransitionPropertyMsgTypeWithCorrelation, _: Void, count: Option[CountMinMax], subject: MapAbleNode[String], inVar: Option[MapAbleNode[String]]) => {
      TransitionPropertyMsgFromSubj(msgTypeWithCorrelation, subject.value, inVar.map(_.value), count)
    }
  )

  val msgToSubj: Parser[CommunicationTransitionProperty] = Parsers.sequence(
    msgTypeWithCorrelationParser,
    kw(PASSParser.KW_TO).skip,
    countMinMaxParser.followedBy(kw(PASSParser.KW_OF)).asOption, // subject count
    idOrStringOrAnyOrAllParser, // subjectID
    (kw(PASSParser.KW_IN) next idOrStringParser).asOption, // channel

    (msgTypeWithCorrelation: TransitionPropertyMsgTypeWithCorrelation, _: Void, count: Option[CountMinMax], subject: MapAbleNode[String], inVar: Option[MapAbleNode[String]]) => {
      TransitionPropertyMsgToSubj(msgTypeWithCorrelation, subject.value, inVar.map(_.value), count)
    }
  )

  val newCorrelation: Parser[CommunicationTransitionProperty] = kw(PASSParser.KW_WITH) next kw(PASSParser.KW_NEW) next kw(PASSParser.KW_CORRELATION) next idOrStringParser map (
    (from: MapAbleNode[String]) => TransitionPropertyWithNewCorrelation(from.value)
    )

  val withContentOf: Parser[CommunicationTransitionProperty] = kw(PASSParser.KW_WITH) next kw(PASSParser.KW_CONTENT) next kw(PASSParser.KW_OF) next idOrStringParser map (
    (from: MapAbleNode[String]) => TransitionPropertyWithContentOf(from.value)
    )

  private val communicationTransitionPropertyParsers: Set[Parser[CommunicationTransitionProperty]] = Set(
    withContentOf,

    newCorrelation,

    // TODO: enforce msgCount=1 <=> message v msgCount>1 <=> messages ?
    kw(PASSParser.KW_STORE) next Parsers.or(Seq(kw(PASSParser.KW_MESSAGES), kw(PASSParser.KW_MESSAGE)): _*) next kw(PASSParser.KW_IN) next STRING map ((from: MapAbleNode[String]) => TransitionPropertyStoreMessagesIn(from.value)),

    kw(PASSParser.KW_STORE) next kw(PASSParser.KW_RECEIVER) next kw(PASSParser.KW_IN) next STRING map ((from: MapAbleNode[String]) => TransitionPropertyStoreReceiverIn(from.value)),

    msgFromSubj,
    msgToSubj
  )


  val transitionPropertyNode: Parser[TransitionProperty] = Parsers.or(transitionsPropertyParsers.toSeq: _*)
  val communicationTransitionPropertyNode: Parser[CommunicationTransitionProperty] = Parsers.or(communicationTransitionPropertyParsers.toSeq: _*)

  // (property1 "property 2" (property3 (4)))
  val transitionProperties: Parser[Seq[TransitionProperty]] = transitionPropertyNode.many1().asSeq.between(lParen, rParen).optional(Seq.empty)

  // [commProperty1 "commProperty 2" (commProperty3 (4))]
  val transitionCommunicationProperties: Parser[CommunicationTransitionNode] = communicationTransitionPropertyNode.many1().asSeq.between(lParenSqr, rParenSqr).map(CommunicationTransitionNode.PARSER)

  // NAME [commProperty1 "commProperty 2"] (property1 "property 2") -> NEXT_STATE
  val transition: Parser[TransitionNode] = Parsers.sequence(
    idOrStringParser.asOption, // transition name
    transitionCommunicationProperties.asOption,
    transitionProperties,
    kw("->") next idOrStringParser, // next StateID

    TransitionNode.PARSER
  )


  private val statePropertyParsers: Set[Parser[StateProperty]] = Set(
    kw(PASSParser.KW_WITH) next kw(PASSParser.KW_PRIORITY) next NUMBER map (
      (from: MapAbleNode[Int]) => StatePropertyPriority(from.value)
      )
  )

  val statePropertyNode: Parser[StateProperty] = Parsers.or(statePropertyParsers.toSeq: _*)

  // (property1, "property 2", 42, [1, 2], {3, 4}, {5 -> 6})
  val stateArguments: Parser[Seq[MapAbleNode[_]]] = MapAbleParser.sepBy1(comma).asSeq.between(lParen, rParen)
  // (foo, "bar")
  val stringArguments: Parser[Seq[MapAbleNode[String]]] = idOrStringParser.sepBy1(comma).asSeq.between(lParen, rParen)

  // STATE: ACTIONS
  val states: Parser[StateNode] = Parsers.sequence[MapAbleNode[String], Option[MapAbleNode[String]], MapAbleNode[String], Option[Seq[StateProperty]], Option[Seq[MapAbleNode[_]]], Option[Seq[TransitionNode]], StateNode](
    idOrStringParser, // StateID
    STRING.asOption followedBy kw(":"), // Label
    idOrStringParser, // NodeType or Function name
    statePropertyNode.many().asSeq.asOption,
    stateArguments.asOption,
    Parsers.or[Seq[TransitionNode]](
      transition.atomic map[Seq[TransitionNode]] (transition => Seq(transition)),
      transition.many().asSeq.between(lParenCurl, rParenCurl)
    ).asOption,

    StateNode.PARSER
  )

  // ("property 1", "property 2")
  val macroArguments: Parser[Seq[MapAbleNode[String]]] = STRING.sepBy1(comma).asSeq.between(lParen, rParen)

  val macroParser: Parser[MacroNode] = Parsers.sequence(
    kw(KW_MACRO) next idOrStringParser, // MacroID
    macroArguments.asOption followedBy lParenCurl,
    params.many().asSeq,
    states.many().asSeq followedBy rParenCurl,

    MacroNode.PARSER
  )

  val processMacroParser: Parser[MacroNode] = Parsers.sequence(
    kw(KW_MACRO) next idOrStringParser, // MacroID
    macroArguments.asOption followedBy lParenCurl,
    params.many().asSeq,
    states.many().asSeq followedBy rParenCurl,

    MacroNode.PARSERProcessMacro
  )


  val subjectParser: Parser[SubjectNode] = Parsers.sequence(
    kw(KW_SUBJECT) next idOrStringParser followedBy lParenCurl,
    params.many().asSeq,
    macroParser.many().asSeq followedBy rParenCurl,

    SubjectNode.PARSER
  )

  val interfaceSubjectParser: Parser[SubjectNode] = Parsers.sequence(
    kw(KW_INTERFACE_SUBJECT) next idOrStringParser,
    kw(KW_IS) next idOrStringOrAnyParser,
    kw(KW_IN) next idOrStringOrAnyParser,

    SubjectNode.PARSERInterfaceSubject
  )


  val processNodeParser: Parser[ProcessNode] = kw(KW_PROCESS).next(
    Parsers.sequence[MapAbleNode[String], Seq[CustomNode], ProcessNode](
      idOrStringParser,
      Parsers.or[CustomNode](Seq(dataParser, subjectParser, interfaceSubjectParser, processMacroParser): _*).many().asSeq.between(lParenCurl, rParenCurl),

      ProcessNode.PARSER
    ))

  // END custom parameters

  def parseProcesses(source: String): Set[ProcessNode] = {
    logger.debug("loadProcess || Source: \n" + source)

    // lex, parse & contextualise
    val _parser: Parser[Set[ProcessNode]] = processNodeParser.many().asSet.from(TOKENIZER, IGNORED)

    val processes: Set[ProcessNode] = _parser.parse(source)

    logger.debug("loadProcess || ProcessNodes: " + processes)

    processes
  }
}
