package de.athalis.pass.parser.graphml.parser

import java.io.{File, StringReader}

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import de.athalis.pass.parser.ast.MapAbleNode
import de.athalis.pass.parser.ast.pass._
import StateNode.StateType._
import de.athalis.pass.parser.graphml.Helper._
import de.athalis.pass.parser.graphml.PASSHelper
import de.athalis.pass.parser.graphml.structure._

import scala.collection.immutable.Seq
import scala.xml.{Elem, XML}
import org.jparsec.error.ParserException

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

  def parseTransition(subjectId: String, x: Edge, msgTypes: MsgTypes, stateIdMapping: Map[String, StateNode]): (StateNode, TransitionNode) = {
    logger.trace("parseTransition for '{}': {}", Seq(subjectId, x): _*)

    val labelTextOption: Option[String] = x.getLabel

    if (x.label.flatMap(_.iconData).isDefined) throw new IllegalArgumentException("iconData present, will be ignored: " + x)

    val sourceNodeId = x.source.id
    val targetNodeId = x.target.id

    logger.trace("sourceNodeId: {}", sourceNodeId)
    logger.trace("targetNodeId: {}", targetNodeId)

    val sourceState: StateNode = stateIdMapping(sourceNodeId)
    val targetState: StateNode = stateIdMapping(targetNodeId)

    logger.trace("sourceState: {}", sourceState.mkString)
    logger.trace("targetState: {}", targetState.mkString)

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
            throw new InternalError("undefined Function requiring arguments from edge")
          }
        }
        else {
          throw new IllegalArgumentException("unknown Function: " + sourceState.function)
        }
      }
      else if (sourceState.stateType == Send) {
        val props = GraphMLJParser.parseSendTransitionLabel(labelText)

        val key = (subjectId, props.subject)

        if (!msgTypes.contains(key)) {
          logger.warn("msgTypes has no key for {}", key)
          throw new IllegalArgumentException("there are no messages defined to be send from '" + subjectId + "' to '" + props.subject + "'")
        }
        else if (!msgTypes(key).contains(props.msgType)) {
          logger.warn("{} does not contain {}", Seq(msgTypes(key), props.msgType): _*)
          throw new IllegalArgumentException("message type '" + props.msgType + "' is not defined to be send from '" + subjectId + "' to '" + props.subject + "'. defined msgTypes: " + msgTypes)
        }

        // TODO: may store which keys have been used in order to inform about unused message types

        transition.communicationProperties = Some(props)
      }
      else if (sourceState.stateType == Receive) {
        val props = GraphMLJParser.parseReceiveTransitionLabel(labelText)

        val key = (props.subject, subjectId)

        if (!msgTypes.contains(key)) {
          logger.warn("msgTypes has no key for {}", key)
          throw new IllegalArgumentException("there are no messages defined to be received by '" + subjectId + "' from '" + props.subject + "'")
        }
        else if (!msgTypes(key).contains(props.msgType)) {
          logger.warn("{} does not contain {}", Seq(msgTypes(key), props.msgType): _*)
          throw new IllegalArgumentException("message type '" + props.msgType + "' is not defined to be received by '" + subjectId + "' from '" + props.subject + "'. defined msgTypes: " + msgTypes)
        }

        transition.communicationProperties = Some(props)
      }
    }
    else {
      if (Seq(FunctionState).contains(sourceState.stateType)) {
        if (functionsWithArgumentsFromEdge.contains(sourceState.function)) {
          logger.trace("transition: {}", x)
          logger.trace("label: {}", x.label)
          throw new IllegalArgumentException("edge without label, but belongs to a Function that requires one")
        }
      }
      else if (Seq(Send, Receive).contains(sourceState.stateType)) {
        logger.trace("transition: {}", x)
        logger.trace("label: {}", x.label)
        throw new IllegalArgumentException("edge without label, but belongs to a Function that requires one")
      }
    }

    if (transitionType == TransitionType.TIMEOUT && transition.timeout.isEmpty) {
      logger.trace("transition: {}", x)
      logger.trace("label: {}", x.label)
      throw new IllegalArgumentException("timeout transition, but no timeout given")
    }

    if (prio.isDefined) {
      transition.priority = prio.get
    }

    transitionType match {
      case TransitionType.NORMAL => Unit
      case TransitionType.CANCEL => { transition.cancel = true }
      case TransitionType.AUTO => { transition.auto = true }
      case TransitionType.TIMEOUT if transition.timeout.isEmpty => { throw new IllegalArgumentException("timeout transition, timeout overwritten with: " + transition.timeout) }
      case TransitionType.TIMEOUT => Unit // seems to be right
    }

    lineType match {
      case Some("line") => Unit
      case Some("dashed") => { transition.hidden = true }
      case x => { throw new IllegalArgumentException("unknown lineType: " + x) }
    }

    (sourceState, transition)
  }

  def getStateType(x: Node): StateType = {
    x.BPMNType match {
      case Some("ACTIVITY_TYPE")    if (x.BPMNTaskType == Some("TASK_TYPE_SEND"))    => Send
      case Some("ACTIVITY_TYPE")    if (x.BPMNTaskType == Some("TASK_TYPE_RECEIVE")) => Receive
      case Some("ACTIVITY_TYPE")    if (x.BPMNTaskType == Some("TASK_TYPE_SERVICE")) => InternalAction
      case Some("ACTIVITY_TYPE")    if (x.BPMNTaskType == Some("TASK_TYPE_SCRIPT"))  => FunctionState
      case Some("EVENT_TYPE_PLAIN") if (x.BPMNTaskType == None)                      => End
      case _ => {
        throw new Exception("unknown BPMNTask: " + x.BPMNType + " - " + x.BPMNTaskType)
      }
    }
  }

  def parseState(x: Node, tempId: String): StateNode = {
    val id: String = x.label.flatMap(x => x("ID")).getOrElse(tempId)
    val prio: Option[String] = x.label.flatMap(x => x("PRIO"))

    val stateType: StateType = getStateType(x)

    val stateNode = new StateNode(id, Some(x))
    stateNode.stateType = stateType

    if (prio.isDefined) {
      stateNode.priority = prio.get.trim.toInt
    }

    stateNode.label = x.getLabel

    if (stateType == FunctionState) {
      if (stateNode.label.isEmpty) throw new IllegalArgumentException("predefinedFunction state, but no label")

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
            case ex: ParserException => throw new IllegalArgumentException("predefinedFunction state, but unable to parse predefined function: '" + labelText + "'", ex)
          }

        val predefinedFunction: String = idArgs._1.value
        val functionArguments: scala.Seq[Any] = idArgs._2.map(_.value)

        if (functionsWithArgumentsFromNode.contains(predefinedFunction)) {
          stateNode.function = predefinedFunction
          stateNode.functionArguments = Some(functionArguments)
        }
        else {
          throw new IllegalArgumentException("unknown predefinedFunction with arguments  from state: '" + predefinedFunction + "' (arguments: '" + functionArguments + "')")
        }
      }
    }
    else if (stateType == End) {
      stateNode.label match {
        case None => {}
        case Some(text) => {
          stateNode.functionArguments = Some(Seq(text))
        }
      }
    }

    stateNode
  }

  def parseMacro(s: SubjectNode, x: Graph, msgTypes: MsgTypes, labelText: String = "Main"): (Map[String, StateNode], Seq[MacroNode]) = {
    logger.trace("parseMacro: {}", labelText)


    val idArgs: (MapAbleNode[String], scala.Seq[MapAbleNode[String]])= try {
      GraphMLJParser.parseIdOptionalStringArgsLabel(labelText)
    }
    catch {
      case ex: ParserException => throw new IllegalArgumentException("macro node, but unable to parse id / arguments: '" + labelText + "'", ex)
    }

    val macroName: String = idArgs._1.value
    val macroArguments: scala.Seq[String] = idArgs._2.map(_.value)

    val m = new MacroNode(macroName, false)

    if (macroArguments.nonEmpty) m.arguments = Some(macroArguments)

    var macros = Seq(m)

    val allStartNodes: Seq[Node] = x.nodes.filter({ n =>
      n.genericNode.flatMap(_.borderStyle).map(_.width) == Some(3.0)
    })

    val (majorStartNodes: Seq[Node], minorStartNodes: Seq[Node]) = allStartNodes.partition({ n =>
      n.genericNode.flatMap(_.borderStyle).map(_.typ) == Some("line")
    })

    if (majorStartNodes.size != 1) throw new Exception("There must be excactly one major start node, found: " + majorStartNodes.map(_.mkString) + " for Macro '" + macroName + "' of '" + s.id + "'")
    if (minorStartNodes.nonEmpty) throw new Exception("There must be no minor start nodes, found: " + minorStartNodes.map(_.mkString) + " for Macro '\" + name + \"' of '" + s.id + "'")

    val startNode = majorStartNodes.head

    var stateIdMapping = Map.empty[String, StateNode]

    var i: Int = 0
    for (n <- x.nodes) {
      if (PASSHelper.isMacro(n)) {
        // TODO: sepatare parseMacro into a parseMainMacro ?
        val labelText = n.label.get.text
        val (a, b) = parseMacro(s, n.subgraph.get, msgTypes, labelText)
        stateIdMapping ++= a // edges belonging to the subgraph could be in this level
        macros ++= b
      }
      else {
        val tempId = "NODE-" + i
        i += 1

        logger.trace("")
        logger.trace("")

        logger.trace("parseState: {}", n)

        val stateNode = parseState(n, tempId)
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

      val (sourceNode, transitionNode) = parseTransition(s.id, e, msgTypes, stateIdMapping)

      logger.trace("adding transition to {}: {}", Seq(sourceNode, transitionNode.mkString): _*)
      sourceNode.addOutgoingTransition(transitionNode)
    }

    (stateIdMapping, macros)
  }

  def parseExternalSubject(x: Node): SubjectNode = {
    val (id, params) = GraphMLJParser.parseSubjectLabelRich(x.getLabel.get)
    val s = new SubjectNode(id, true)

    assert(params.contains("SubRef"))

    val subRef = params("SubRef").asInstanceOf[scala.collection.mutable.ArrayBuffer[String]] // FIXME: ugly cast..

    assert(subRef.size == 2)

    s.externalProcessID = subRef(0)
    s.externalSubjectID = subRef(1)

    s
  }

  def parseInternalSubject(x: Node, msgTypes: MsgTypes, ipDefault: Option[Int]): SubjectNode = {
    val (id, params) = GraphMLJParser.parseSubjectLabelRich(x.getLabel.get)
    // NOTE: x.getData left for capability, should be removed
    val ipSize: Option[Int] = x.getData[Int](IPKeyName).flatMap(_.value).orElse(params.get("IPSize").map(_.asInstanceOf[Int])).orElse(ipDefault)
    val isStartSubject = x.groupNodes.exists(_.borderStyle.map(_.width).contains(3.0))

    val s = new SubjectNode(id)
    if (ipSize.isDefined) { s.setParameter("InputPool", ipSize.get) }
    s.setParameter("StartSubject", isStartSubject)

    logger.trace("")
    logger.trace("")

    if (x.subgraph.isEmpty) throw new IllegalArgumentException("expected a subgraph!")

    logger.trace("parse subgraph: {}", x.subgraph)

    val (_, macros) = parseMacro(s, x.subgraph.get, msgTypes)

    for (m <- macros) {
      logger.trace("adding macro: {}", m.mkString)
      s.addMacro(m)
    }

    s
  }

  def parseProcess(g: GraphML, name: String = "GraphMLImport"): (ProcessNode, MsgTypes) = {
    logger.trace("parseProcess '{}': {}", Seq(name, g): _*)

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

    val edgesLabel: Map[(String, String), Set[YEdgeLabel]] = edges.mapValues(s => s.map(_.label).flatten.toSet)
    val msgTypeBoxes: Map[(String, String), Set[String]] = edgesLabel.mapValues(_.map(_.text))

    def parseMsgTypes(x: String): Set[String] = {
      x.split("\n").map(_.trim).filterNot(_.isEmpty).map(_.drop(1)).map(GraphMLJParser.cleanQuotes).toSet
    }

    // note: lazy mapping
    val msgTypes: MsgTypes = msgTypeBoxes.mapValues(_.map(parseMsgTypes).flatten)


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
          parseInternalSubject(x, msgTypes, ipDefault)
        }
        else if (PASSHelper.isExternalSubject(x)) {
          parseExternalSubject(x)
        }
        else {
          throw new IllegalArgumentException("Subject '"+x.getLabel.get+"' is neither internal nor external!")
        }

      logger.trace("adding subject: {}", subjectNode.mkString)

      p.addSubject(subjectNode)
    }

    (p, msgTypes)
  }



  def loadProcesses(file: File): Set[(ProcessNode, MsgTypes)] = {
    logger.info("loading xml file: {}", file)

    val source: Elem = XML.loadFile(file)

    loadProcesses(source, file.getName)
  }

  def loadProcesses(source: String, sourceName: String): Set[(ProcessNode, MsgTypes)] = {
    val sourceElem: Elem = XML.load(new StringReader(source))

    loadProcesses(sourceElem, sourceName)
  }

  def loadProcesses(source: Elem, sourceName: String): Set[(ProcessNode, MsgTypes)] = {
    val g: GraphML = parseGraphML(source)

    val gPruned: GraphML = PASSHelper.pruneGraphML(g)

    val hasOnlyProcesses = gPruned.graph.nodes.forall(n => (PASSHelper.isProcess(n) || PASSHelper.isData(n)))
    val hasOnlySubjects = gPruned.graph.nodes.forall(n => (PASSHelper.isSubject(n) || PASSHelper.isData(n)))

    if (hasOnlySubjects) {
      Set(parseProcess(gPruned, sourceName))
    }
    else if (hasOnlyProcesses) {
      val processGraphs: Seq[(GraphML, String)] = gPruned.graph.nodes.map(n => (gPruned.copy(graph = n.subgraph.get), n.getLabel.get))

      processGraphs.map(t => {
        val (gProcess: GraphML, processName: String) = t

        parseProcess(gProcess, processName)
      }).toSet
    }
    else {
      throw new IllegalArgumentException("mixed process and subject(group) nodes on root level")
    }
  }

}
