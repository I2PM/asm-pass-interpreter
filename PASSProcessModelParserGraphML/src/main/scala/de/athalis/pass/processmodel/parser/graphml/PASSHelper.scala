package de.athalis.pass.processmodel.parser.graphml

import de.athalis.pass.processmodel.parser.graphml.structure._

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import org.slf4j.LoggerFactory

import scala.annotation.elidable
import scala.annotation.elidable.ASSERTION
import scala.collection.immutable._

object PASSHelper {
  private val logger = LoggerFactory.getLogger(PASSHelper.getClass)

  private val fallbackConfig: Config = ConfigFactory.load(this.getClass.getClassLoader)
  private val config: Config = fallbackConfig.getConfig("pass-parser.graphml").withFallback(fallbackConfig)

  private val failOnWarn = config.getBoolean("fail-on-warning")

  private def warn(msg: String, args: Object*): Unit = {
    logger.warn(msg, args: _*)
    if(failOnWarn) throw new Exception("WARN: " + msg.replaceAll("""\{\}""", """\%s""").format(args))
  }

  // START small helpers

  private def isFolder(n: Node): Boolean = {
    (n.yFoldertype.contains("folder") || n.yFoldertype.contains("group"))
  }

  private def nonFolder(n: Node): Boolean = {
    (n.yFoldertype.isEmpty)
  }

  private def isOpenedFolder(n: Node): Boolean = {
    (n.yFoldertype.contains("group"))
  }

  private def isClosedFolder(n: Node): Boolean = {
    (n.yFoldertype.contains("folder"))
  }


  private def isComment(n: Node): Boolean = {
    (nonFolder(n) && n.BPMNType.contains("ARTIFACT_TYPE_ANNOTATION"))
  }

  def isData(n: Node): Boolean = {
    (nonFolder(n) && n.BPMNType.contains("ARTIFACT_TYPE_DATA_OBJECT"))
  }

  private def isState(n: Node): Boolean = {
    (nonFolder(n) && (n.BPMNType.contains("ACTIVITY_TYPE") || n.BPMNType.contains("EVENT_TYPE_PLAIN")))
  }

  def isProcess(n: Node): Boolean = {
    (isFolder(n) && n.BPMNType.contains("ACTIVITY_TYPE") && n.BPMNTaskType.contains("TASK_TYPE_ABSTRACT") && n.description.contains("Process"))
  }

  def isSubjectGroup(n: Node): Boolean = {
    (isFolder(n) && n.BPMNType.isEmpty && n.description.isEmpty)
  }

  def isSubject(n: Node): Boolean = {
    (isFolder(n) && n.BPMNType.isEmpty && n.description.contains("Subject"))
  }

  def isExternalSubject(n: Node): Boolean = {
    (isSubject(n) && n.groupNodes.forall(_.borderStyle.map(_.typ).contains("dashed")))
  }

  def isInternalSubject(n: Node): Boolean = {
    (isSubject(n) && n.groupNodes.forall(_.borderStyle.map(_.typ).contains("line")))
  }

  def isMacro(n: Node): Boolean = {
    (isFolder(n) && n.BPMNType.contains("ACTIVITY_TYPE") && n.BPMNTaskType.contains("TASK_TYPE_ABSTRACT") && n.description.contains("Macro"))
  }


  private def isSequenceFlowEdge(e: Edge): Boolean = {
    (e.BPMNType.contains("CONNECTION_TYPE_SEQUENCE_FLOW"))
  }

  private def isMessageFlowEdge(e: Edge): Boolean = {
    (e.BPMNType.contains("CONNECTION_TYPE_MESSAGE_FLOW"))
  }

  private def isCommentEdge(e: Edge): Boolean = {
    (e.BPMNType.contains("CONNECTION_TYPE_ASSOCIATION") || e.BPMNType.contains("CONNECTION_TYPE_DIRECTED_ASSOCIATION"))
  }

  private def mergeGraphs(g: Graph, x: Seq[Graph]): Graph = {
    val newNodes: Seq[Node] = g.nodes ++ x.flatMap(_.nodes)
    val newEdges: Seq[Edge] = g.edges ++ x.flatMap(_.edges)
    g.copy(nodes = newNodes, edges = newEdges)
  }

  // END small helpers

  def pruneGraphML(g: GraphML): GraphML = {
    logger.trace("pruneGraphML: {}", g)

    val g1 = pruneGraph(g.graph)

    val g2 = liftSubjectsOutOfSubjectGroups(g1)

    assertNoSubjectGroups(g2)

    val (g3, remainingEdges) = moveEdges(g2)

    assertEdges(g3)
    assert(remainingEdges.isEmpty)


    val hasOnlyProcessModels = g3.nodes.forall(n => (isProcess(n) || isData(n)))
    val hasOnlySubjects = g3.nodes.forall(n => (isSubject(n) || isData(n)))

    if (hasOnlyProcessModels) {
      assert(g3.edges.isEmpty)
      assert(g3.nodes.forall(_.subgraph.isDefined))
      assert(g3.nodes.forall(_.getLabel.isDefined))
    }
    else {
      assert(hasOnlySubjects)
    }

    g.copy(graph = g3)
  }

  @elidable(ASSERTION)
  def assertEdges(g: Graph): Unit = {
    g.nodes.flatMap(_.subgraph).foreach(assertEdges)

    val missingSources: Seq[NodeReference] = g.edges.map(_.source).filterNot(g.nodes.map(_.reference).contains)

    val missingTargets: Seq[NodeReference] = g.edges.map(_.target).filterNot(g.nodes.map(_.reference).contains)

    assert((missingSources ++ missingTargets).isEmpty)
  }

  @elidable(ASSERTION)
  def assertNoSubjectGroups(g: Graph): Unit = {
    g.nodes.flatMap(_.subgraph).foreach(assertNoSubjectGroups)

    val subjectGroups = g.nodes.filter(isSubjectGroup)

    assert(subjectGroups.isEmpty)
  }

  def liftSubjectsOutOfSubjectGroups(g: Graph): Graph = {
    val (processNodes: Seq[Node], otherNodes: Seq[Node]) = g.nodes.partition(isProcess)

    assert(processNodes.forall(_.subgraph.isDefined))

    val processNodesNew: Seq[Node] = processNodes.map(pn => pn.copy(subgraph = Some(liftSubjectsOutOfSubjectGroups(pn.subgraph.get))))


    val (subjectGroups, remainingNodes) = otherNodes.partition(isSubjectGroup)

    val subjectGraphs = subjectGroups.flatMap(_.subgraph)

    val g2: Graph = g.copy(nodes = (processNodesNew ++ remainingNodes))

    mergeGraphs(g2, subjectGraphs)
  }

  def pruneGraph(g: Graph): Graph = {
    val newEdges1: Seq[Edge] = g.edges.filter(keepEdge)
    val (newNodes1: Seq[Node], removeNodes) = g.nodes.partition(keepNode)

    val newEdges2 = newEdges1.filterNot(e => (removeNodes.map(_.reference).contains(e.source) || removeNodes.map(_.reference).contains(e.target)))

    val newNodes2: Seq[Node] = newNodes1.map(n => n.subgraph match {
      case None => n
      case Some(g1) => n.copy(subgraph = Some(pruneGraph(g1)))
    })

    g.copy(nodes = newNodes2, edges = newEdges2)
  }


  def keepNode(n: Node): Boolean = {
    if (isData(n) || isProcess(n) || isSubjectGroup(n) || isSubject(n) || isMacro(n) || isState(n)) {
      true
    }
    else {
      if (!isComment(n)) {
        warn("unknown node, throw away: {}", n)
      }

      assert(isComment(n))

      false
    }
  }

  def keepEdge(e: Edge): Boolean = {
    if (isSequenceFlowEdge(e) || isMessageFlowEdge(e)) {
      true
    }
    else {
      if (!isCommentEdge(e)) warn("unknown edge, throw away: {}", e.mkString)

      assert(isCommentEdge(e))

      false
    }
  }

  private def matchEdges(edges: Seq[Edge], nodeIds: Seq[String]): (Seq[Edge], Seq[Edge]) = edges.partition { e => (nodeIds.contains(e.source.id) && nodeIds.contains(e.target.id)) }

  def moveEdges(g: Graph, additionalEdges: Seq[Edge] = Seq.empty): (Graph, Seq[Edge]) = {
    val (goodEdges, wrongEdges) = matchEdges(g.edges ++ additionalEdges, g.nodes.map(_.id))
    logger.trace("g.nodes: {}", g.nodes.map(_.mkString))
    logger.trace("goodEdges: {}", goodEdges.map(_.mkString))
    logger.trace("wrongEdges: {}", wrongEdges.map(_.mkString))

    var remainingEdges: Seq[Edge] = wrongEdges

    val newNodes = g.nodes.map(n => {
      val newSubGraph: Option[Graph] = n.subgraph.map(gSub => {
        val (g2, remainingEdges2) = moveEdges(gSub, remainingEdges)
        remainingEdges = remainingEdges2
        g2
      })

      n.copy(subgraph = newSubGraph)
    })

    (g.copy(nodes = newNodes, edges = goodEdges), remainingEdges)
  }
}
