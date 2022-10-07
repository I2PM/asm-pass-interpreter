package de.athalis.pass.processmodel.parser.graphml.parser

import de.athalis.pass.processmodel.parser.ast.node.MapAbleNode
import de.athalis.pass.processmodel.parser.ast.node.pass.StateNode.StateType._
import de.athalis.pass.processmodel.parser.ast.node.pass._
import de.athalis.pass.processmodel.parser.graphml.Helper._
import de.athalis.pass.processmodel.parser.graphml.PASSHelper
import de.athalis.pass.processmodel.parser.graphml.structure._

import org.jparsec.error.ParserException

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path

import scala.collection.immutable.Seq
import scala.collection.mutable.ArrayBuffer
import scala.xml.Elem
import scala.xml.XML

object GraphMLParser {
  // (sender, receiver) -> Set[msgType]
  type MsgTypes = Map[(String, String), Set[String]]
  object MsgTypes {
    def empty: MsgTypes = Map.empty[(String, String), Set[String]]
  }

  private sealed abstract class TransitionType
  private object TransitionType {
    case object NORMAL extends TransitionType
    case object AUTO extends TransitionType
    case object TIMEOUT extends TransitionType
    case object CANCEL extends TransitionType

    def getTransitionType(e: Edge): TransitionType = {
      e.lineColor match {
        case None => TransitionType.NORMAL
        case Some(RGBColor.red) => TransitionType.CANCEL
        case Some(RGBColor.green) => TransitionType.AUTO
        case Some(RGBColor.blue) => TransitionType.TIMEOUT
        case Some(x) => { logger.info("unknown lineColor: " + x); TransitionType.NORMAL }
      }
    }
  }

  private val logger: Logger = LoggerFactory.getLogger(GraphMLParser.getClass)

  val IPKeyName = "IP Size"

  val functionsWithArgumentsFromEdge: Seq[String] = Seq("VarMan", "SelectAgents")
  val functionsWithArgumentsFromNode: Seq[String] = Seq("CallMacro", "IsIPEmpty", "CloseIP", "OpenIP")
  val functionsWithoutArguments: Seq[String] = Seq("Tau", "Cancel", "ModalJoin", "ModalSplit", "CloseAllIPs", "OpenAllIPs")
  val predefinedFunctions: Seq[String] = functionsWithArgumentsFromEdge ++ functionsWithArgumentsFromNode ++ functionsWithoutArguments

  def parseTransition(subjectId: String, x: Edge, msgTypes: MsgTypes, stateIdMapping: Map[String, StateNode])(parentLoc: ParserLocation): (StateNode, TransitionNode) = {
    logger.trace("parseTransition for '{}': {}", Seq(subjectId, x): _*)

    val labelTextOption: Option[String] = x.getLabel

    val loc1: ParserLocation = ParserLocation("parseTransition(" + labelTextOption + ")", Some(parentLoc))

    if (x.label.flatMap(_.iconData).isDefined) throw new IllegalArgumentException("iconData present, will be ignored: " + x + " " + loc1)

    val sourceNodeId = x.source.id
    val targetNodeId = x.target.id

    logger.trace("sourceNodeId: {}", sourceNodeId)
    logger.trace("targetNodeId: {}", targetNodeId)

    val sourceState: StateNode = stateIdMapping(sourceNodeId)
    val targetState: StateNode = stateIdMapping(targetNodeId)

    logger.trace("sourceState: {}", sourceState.mkString)
    logger.trace("targetState: {}", targetState.mkString)

    implicit val loc: ParserLocation = ParserLocation("parseTransition(" + sourceState.label.getOrElse(sourceState.id) + " -> " + targetState.label.getOrElse(targetState.id) + ", " + labelTextOption + ")", Some(parentLoc))

    val transitionId: Option[String] = x.label.flatMap(l => l("ID"))

    val transitionLabel: Option[String] = if (transitionId.isDefined) {
      transitionId
    }
    else if (functionsWithArgumentsFromEdge.contains(sourceState.function)) {
      None
    }
    else {
      labelTextOption
    }

    val transition = new TransitionNode(transitionLabel)

    transition.targetStateID = targetState.id

    val prio: Option[Int] = x.label.flatMap(l => l("PRIO").map(_.trim.toInt))

    val lineType: Option[String] = x.lineType

    val transitionType: TransitionType = TransitionType.getTransitionType(x)

    if (transitionType == TransitionType.CANCEL) {
      // nothing to do
    }
    else if (labelTextOption.isDefined) {
      val labelText = labelTextOption.get

      if (transitionType == TransitionType.TIMEOUT) {
        val t: Int = labelText.dropRight(1).toInt
        transition.timeout = Some(t)
      }
      else if (sourceState.stateType == FunctionState) {
        if (functionsWithoutArguments.contains(sourceState.function)) {
          logger.trace("predefinedFunction without arguments")
          //nothing to do
        }
        else if (functionsWithArgumentsFromNode.contains(sourceState.function)) {
          logger.trace("predefinedFunction with arguments from node")
          //nothing to do
        }
        else if (functionsWithArgumentsFromEdge.contains(sourceState.function)) {
          logger.trace("predefinedFunction with arguments from edge")
          if (sourceState.function == "VarMan") {
            val args = GraphMLJParser.parseVarManEdgeLabel(labelText)
            sourceState.functionArguments = Some(args.map(_.value))
          }
          else if (sourceState.function == "SelectAgents") {
            val args = GraphMLJParser.parseSelectAgentsEdgeLabel(labelText)
            sourceState.functionArguments = Some(args.map(_.value))
          }
          else {
            throw new InternalError("undefined Function requiring arguments from edge " + loc)
          }
        }
        else {
          throw new IllegalArgumentException("unknown Function: " + sourceState.function + " " + loc)
        }
      }
      else if (sourceState.stateType == Send) {
        val props = GraphMLJParser.parseSendTransitionLabel(labelText)

        val key = (subjectId, props.subject)

        if (!msgTypes.contains(key)) {
          logger.warn("msgTypes has no key for {}", key)
          throw new IllegalArgumentException("there are no messages defined to be send from '" + subjectId + "' to '" + props.subject + "' " + loc)
        }
        else if (!msgTypes(key).contains(props.msgType)) {
          logger.warn("{} does not contain {}", Seq(msgTypes(key), props.msgType): _*)
          throw new IllegalArgumentException("message type '" + props.msgType + "' is not defined to be send from '" + subjectId + "' to '" + props.subject + "'. defined msgTypes: " + msgTypes + " " + loc)
        }

        // TODO: may store which keys have been used in order to inform about unused message types

        transition.communicationProperties = Some(props)
      }
      else if (sourceState.stateType == Receive) {
        val props = GraphMLJParser.parseReceiveTransitionLabel(labelText)

        val key = (props.subject, subjectId)

        if (!msgTypes.contains(key)) {
          logger.warn("msgTypes has no key for {}", key)
          throw new IllegalArgumentException("there are no messages defined to be received by '" + subjectId + "' from '" + props.subject + "' " + loc)
        }
        else if (!msgTypes(key).contains(props.msgType)) {
          logger.warn("{} does not contain {}", Seq(msgTypes(key), props.msgType): _*)
          throw new IllegalArgumentException("message type '" + props.msgType + "' is not defined to be received by '" + subjectId + "' from '" + props.subject + "'. defined msgTypes: " + msgTypes + " " + loc)
        }

        transition.communicationProperties = Some(props)
      }
    }
    else {
      if (Seq(FunctionState).contains(sourceState.stateType)) {
        if (functionsWithArgumentsFromEdge.contains(sourceState.function)) {
          logger.trace("transition: {}", x)
          logger.trace("label: {}", x.label)
          throw new IllegalArgumentException("edge without label, but belongs to a Function that requires one " + loc)
        }
      }
      else if (Seq(Send, Receive).contains(sourceState.stateType)) {
        logger.trace("transition: {}", x)
        logger.trace("label: {}", x.label)
        throw new IllegalArgumentException("edge without label, but belongs to a Function that requires one " + loc)
      }
    }

    if (transitionType == TransitionType.TIMEOUT && transition.timeout.isEmpty) {
      logger.trace("transition: {}", x)
      logger.trace("label: {}", x.label)
      throw new IllegalArgumentException("timeout transition, but no timeout given " + loc)
    }

    if (prio.isDefined) {
      transition.priority = prio.get
    }

    transitionType match {
      case TransitionType.NORMAL => ()
      case TransitionType.CANCEL => { transition.cancel = true }
      case TransitionType.AUTO => { transition.auto = true }
      case TransitionType.TIMEOUT if transition.timeout.isEmpty => { throw new IllegalArgumentException("timeout transition, timeout overwritten with: " + transition.timeout + " " + loc) }
      case TransitionType.TIMEOUT => () // seems to be right
    }

    lineType match {
      case Some("line") => ()
      case Some("dashed") => { transition.hidden = true }
      case x => { throw new IllegalArgumentException("unknown lineType: " + x + " " + loc) }
    }

    (sourceState, transition)
  }

  def getStateType(x: Node, isInMainMacro: Boolean)(implicit loc: ParserLocation): StateType = {
    x.BPMNType match {
      case Some("ACTIVITY_TYPE")    if (x.BPMNTaskType == Some("TASK_TYPE_SEND"))    => Send
      case Some("ACTIVITY_TYPE")    if (x.BPMNTaskType == Some("TASK_TYPE_RECEIVE")) => Receive
      case Some("ACTIVITY_TYPE")    if (x.BPMNTaskType == Some("TASK_TYPE_SERVICE")) => InternalAction
      case Some("ACTIVITY_TYPE")    if (x.BPMNTaskType == Some("TASK_TYPE_SCRIPT"))  => FunctionState
      case Some("EVENT_TYPE_PLAIN") if (x.BPMNTaskType == None && isInMainMacro)     => Terminate
      case Some("EVENT_TYPE_PLAIN") if (x.BPMNTaskType == None && !isInMainMacro)    => Return
      case _ => {
        throw new Exception("unknown BPMNTask: " + x.BPMNType + " - " + x.BPMNTaskType + " " + loc)
      }
    }
  }

  def parseState(x: Node, tempId: String, isInMainMacro: Boolean)(parentLoc: ParserLocation): StateNode = {
    val id: String = x.label.flatMap(x => x("ID")).getOrElse(tempId)
    implicit val loc: ParserLocation = ParserLocation("parseState(" + id + ")", Some(parentLoc))
    val prio: Option[String] = x.label.flatMap(x => x("PRIO"))

    val stateType: StateType = getStateType(x, isInMainMacro)

    val stateNode = new StateNode(id, Some(x))
    stateNode.stateType = stateType

    if (prio.isDefined) {
      stateNode.priority = prio.get.trim.toInt
    }

    stateNode.label = x.getLabel

    if (stateType == FunctionState) {
      if (stateNode.label.isEmpty) throw new IllegalArgumentException("predefinedFunction state, but no label " + loc)

      val labelText = stateNode.label.get

      if (functionsWithArgumentsFromEdge.contains(labelText)) {
        stateNode.function = labelText
        // functionArguments will be filled from edge
      }
      else if (functionsWithoutArguments.contains(labelText)) {
        stateNode.function = labelText
      }
      else {
        val idArgs: (MapAbleNode[String], scala.Seq[MapAbleNode[Any]]) = try {
            GraphMLJParser.parseIdArgsLabel(labelText)
          }
          catch {
            case ex: ParserException => throw new IllegalArgumentException("predefinedFunction state, but unable to parse predefined function: '" + labelText + "' " + loc, ex)
          }

        val predefinedFunction: String = idArgs._1.value
        val functionArguments: scala.Seq[Any] = idArgs._2.map(_.value)

        if (functionsWithArgumentsFromNode.contains(predefinedFunction)) {
          stateNode.function = predefinedFunction
          stateNode.functionArguments = Some(functionArguments)
        }
        else {
          throw new IllegalArgumentException("unknown predefinedFunction with arguments  from state: '" + predefinedFunction + "' (arguments: '" + functionArguments + "') " + loc)
        }
      }
    }
    else if (stateType == Terminate) {
      stateNode.label match {
        case None => {}
        case Some(text) => {
          stateNode.functionArguments = Some(Seq(text))
        }
      }
    }
    else if (stateType == Return) {
      stateNode.label match {
        case None => {}
        case Some(text) => {
          stateNode.functionArguments = Some(Seq(text))
        }
      }
    }

    stateNode
  }

  def parseMacro(s: SubjectNode, x: Graph, msgTypes: MsgTypes, labelText: String, isMainMacro: Boolean)(parentLoc: ParserLocation): (Map[String, StateNode], Seq[MacroNode]) = {
    logger.trace("parseMacro: {}", labelText)
    implicit val loc: ParserLocation = ParserLocation("parseMacro(" + labelText + ")", Some(parentLoc))

    val idArgs: (MapAbleNode[String], scala.Seq[MapAbleNode[String]])= try {
      GraphMLJParser.parseIdOptionalStringArgsLabel(labelText)
    }
    catch {
      case ex: ParserException => throw new IllegalArgumentException("macro node, but unable to parse id / arguments: '" + labelText + "' " + loc, ex)
    }

    val macroName: String = idArgs._1.value
    val macroArguments: scala.Seq[String] = idArgs._2.map(_.value)

    val m = new MacroNode(macroName, false)

    if (macroArguments.nonEmpty) m.arguments = Some(macroArguments)

    var macros: Seq[MacroNode] = Seq(m)

    val allStartNodes: Seq[Node] = x.nodes.filter({ n =>
      n.genericNode.flatMap(_.borderStyle).map(_.width) == Some(3.0)
    })

    val (majorStartNodes: Seq[Node], minorStartNodes: Seq[Node]) = allStartNodes.partition({ n =>
      n.genericNode.flatMap(_.borderStyle).map(_.typ) == Some("line")
    })

    if (majorStartNodes.size != 1) throw new Exception("There must be exactly one major start node, found: " + majorStartNodes.map(_.mkString) + " for Macro '" + macroName + "' of '" + s.id + "' " + loc)
    if (minorStartNodes.nonEmpty) throw new Exception("There must be no minor start nodes, found: " + minorStartNodes.map(_.mkString) + " for Macro '\" + name + \"' of '" + s.id + "' " + loc)

    val startNode = majorStartNodes.head

    var stateIdMapping = Map.empty[String, StateNode]

    var i: Int = 0
    for (n <- x.nodes) {
      if (PASSHelper.isMacro(n)) {
        // TODO: separate parseMacro into a parseMainMacro ?
        val labelText = n.label.get.text
        val (a, b): (Map[String, StateNode], Seq[MacroNode]) = parseMacro(s, n.subgraph.get, msgTypes, labelText, isMainMacro = false)(loc)
        stateIdMapping ++= a // edges belonging to the subgraph could be in this level
        macros ++= b
      }
      else {
        val tempId = "NODE-" + i
        i += 1

        logger.trace("")
        logger.trace("")

        logger.trace("parseState: {}", n)

        val stateNode: StateNode = parseState(n, tempId, isMainMacro)(loc)
        stateIdMapping += (n.id -> stateNode)

        if (n == startNode) {
          m.setParameter("StartState", stateNode.id)
        }

        m.addState(stateNode)
      }
    }

    for (e <- x.edges) {
      logger.trace("")
      logger.trace("")

      logger.trace("parseTransition: {}", e)

      val (sourceNode, transitionNode): (StateNode, TransitionNode) = parseTransition(s.id, e, msgTypes, stateIdMapping)(loc)

      logger.trace("adding transition to {}: {}", Seq(sourceNode, transitionNode.mkString): _*)
      sourceNode.addOutgoingTransition(transitionNode)
    }

    (stateIdMapping, macros)
  }

  def parseExternalSubject(x: Node)(parentLoc: ParserLocation): SubjectNode = {
    implicit val loc: ParserLocation = ParserLocation("parseExternalSubject(" + x.getLabel + ")", Some(parentLoc))
    val (id, params) = GraphMLJParser.parseSubjectLabelRich(x.getLabel.get)
    val s = new SubjectNode(id, true)

    if (!params.contains("SubRef")) {
      throw new Exception("ExternalSubject needs a SubRef")
    }

    val subRef: (String, String) = params("SubRef") match {
      case Seq(p, s) if (p.isInstanceOf[String] && s.isInstanceOf[String]) => (p.asInstanceOf[String], s.asInstanceOf[String])
      case ArrayBuffer(p, s) if (p.isInstanceOf[String] && s.isInstanceOf[String]) => (p.asInstanceOf[String], s.asInstanceOf[String])
      case x => throw new Exception("unknown SubRef, expected list of two Strings, but got: " + x)
    }

    s.externalProcessID = subRef._1
    s.externalSubjectID = subRef._2

    s
  }

  def parseInternalSubject(x: Node, msgTypes: MsgTypes, ipDefault: Option[Int])(parentLoc: ParserLocation): SubjectNode = {
    implicit val loc: ParserLocation = ParserLocation("parseInternalSubject(" + x.getLabel + ")", Some(parentLoc))
    val (id, params) = GraphMLJParser.parseSubjectLabelRich(x.getLabel.get)
    // NOTE: x.getData left for capability, should be removed
    val ipSize: Option[Int] = x.getData[Int](IPKeyName).flatMap(_.value).orElse(params.get("IPSize").map(_.asInstanceOf[Int])).orElse(ipDefault)
    val isStartSubject = x.groupNodes.exists(_.borderStyle.map(_.width).contains(3.0))

    val s = new SubjectNode(id)
    if (ipSize.isDefined) { s.setParameter("InputPool", ipSize.get) }
    s.setParameter("StartSubject", isStartSubject)

    logger.trace("")
    logger.trace("")

    if (x.subgraph.isEmpty) throw new IllegalArgumentException("expected a subgraph! " + loc)

    logger.trace("parse subgraph: {}", x.subgraph)

    val (_, macros): (Map[String, StateNode], Seq[MacroNode]) = parseMacro(s, x.subgraph.get, msgTypes, "Main", isMainMacro = true)(loc)

    for (m <- macros) {
      logger.trace("adding macro: {}", m.mkString)
      s.addMacro(m)
    }

    s
  }

  def parseProcessModel(g: GraphML, name: String = "GraphMLImport")(parentLoc: ParserLocation): (ProcessNode, MsgTypes) = {
    implicit val loc: ParserLocation = ParserLocation("parseProcessModel(" + name + ")", Some(parentLoc))
    logger.trace("parseProcessModel '{}': {}", Seq(name, g): _*)

    val p = new ProcessNode(name)

    val ipDefault = g.findKeyByName[Int]("node", IPKeyName).flatMap(_.default)

    val data: Option[Node] = g.graph.nodes.filter(PASSHelper.isData).headOption
    val subjects: Seq[Node] = g.graph.nodes.filter(PASSHelper.isSubject)

    assert(g.graph.nodes.lengthCompare(subjects.size + data.size) == 0)

    val subjectIdMappingSeq: Seq[(String, String)] = subjects.map(x => (x.id, GraphMLJParser.parseSubjectLabelRich(x.getLabel.get)._1))
    val subjectIdMapping: Map[String, String] = subjectIdMappingSeq.toMap

    val edges_a: Seq[((String, String), Edge)] = g.graph.edges.map(e => ((e.source.id, e.target.id), e))
    val edges_b: Map[(String, String), Set[Edge]] = edges_a.toSet[((String, String), Edge)].toSetMap
    val edges: Map[(String, String), Set[Edge]] = edges_b.map(x => ((subjectIdMapping(x._1._1), subjectIdMapping(x._1._2)), x._2))

    val edgesLabel: Map[(String, String), Set[YEdgeLabel]] = edges.mapValues(s => s.map(_.label).flatten.toSet).toMap
    val msgTypeBoxes: Map[(String, String), Set[String]] = edgesLabel.mapValues(_.map(_.text)).toMap

    def parseMsgTypes(x: String): Set[String] = {
      val locParseMsgTypes: ParserLocation = ParserLocation("parseMsgTypes(" + x + ")", Some(loc))
      x.split("\\R").map(_.trim).filterNot(_.isEmpty).map(_.drop(1)).map(x => GraphMLJParser.cleanQuotes(x)(locParseMsgTypes)).toSet
    }

    // note: lazy mapping
    val msgTypes: MsgTypes = msgTypeBoxes.mapValues(_.map(parseMsgTypes).flatten).toMap


    if (data.isDefined) {
      val dataText: String = data.get.getLabel.get
      val dn: DataNode = GraphMLJParser.parseDataNode(dataText)
      p.addData(dn)
    }


    for (x <- subjects) {
      logger.trace("")
      logger.trace("")
      logger.trace("parseSubject: {}", x)

      val subjectNode: SubjectNode =
        if (PASSHelper.isInternalSubject(x)) {
          parseInternalSubject(x, msgTypes, ipDefault)(loc)
        }
        else if (PASSHelper.isExternalSubject(x)) {
          parseExternalSubject(x)(loc)
        }
        else {
          throw new IllegalArgumentException("Subject '"+x.getLabel.get+"' is neither internal nor external! " + loc)
        }

      logger.trace("adding subject: {}", subjectNode.mkString)

      p.addSubject(subjectNode)
    }

    (p, msgTypes)
  }


  def loadProcessModels(path: Path): Set[(ProcessNode, MsgTypes)] = {
    logger.info("loading xml file: {}", path)

    val source: Elem = XML.load(Files.newBufferedReader(path))

    loadProcessModels(source, path.getFileName.toString)
  }

  def loadProcessModels(reader: Reader, sourceName: String): Set[(ProcessNode, MsgTypes)] = {
    val sourceElem: Elem = XML.load(reader)

    loadProcessModels(sourceElem, sourceName)
  }

  // TODO: return PASSProcessModelCollection
  def loadProcessModels(source: Elem, sourceName: String): Set[(ProcessNode, MsgTypes)] = {
    implicit val loc: ParserLocation = ParserLocation("loadProcessModels(" + sourceName + ")", None)
    val g: GraphML = parseGraphML(source)

    val gPruned: GraphML = PASSHelper.pruneGraphML(g)

    val hasOnlyProcessModels = gPruned.graph.nodes.forall(n => (PASSHelper.isProcess(n) || PASSHelper.isData(n)))
    val hasOnlySubjects = gPruned.graph.nodes.forall(n => (PASSHelper.isSubject(n) || PASSHelper.isData(n)))

    if (hasOnlySubjects) {
      Set(parseProcessModel(gPruned, sourceName)(loc))
    }
    else if (hasOnlyProcessModels) {
      val processModelsGraphs: Seq[(GraphML, String)] = gPruned.graph.nodes.map(n => (gPruned.copy(graph = n.subgraph.get), n.getLabel.get))

      processModelsGraphs.map(t => {
        val (gProcessModel: GraphML, processModelName: String) = t

        parseProcessModel(gProcessModel, processModelName)(loc)
      }).toSet
    }
    else {
      throw new IllegalArgumentException("mixed process and subject(group) nodes on root level " + loc)
    }
  }

}
