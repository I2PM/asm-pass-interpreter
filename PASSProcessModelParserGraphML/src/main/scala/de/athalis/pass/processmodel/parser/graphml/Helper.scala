package de.athalis.pass.processmodel.parser.graphml

import de.athalis.pass.processmodel.parser.graphml.structure._

import org.slf4j.LoggerFactory

import scala.collection.immutable._
import scala.reflect.ClassTag
import scala.xml.{Node => XMLNode}
import scala.xml.{NodeSeq => XMLNodeSeq}

object Helper {
  private val logger = LoggerFactory.getLogger(Helper.getClass)

  case class ParserLocation(pos: String, parent: Option[ParserLocation]) {
    override def toString: String = {
      val tail = if (parent.isDefined) { " " + parent.get.toString} else { "" }
      "AT " + pos + tail
    }
  }

  implicit class ColonString(self: String) {
    val pre: String = self.split(":")(0)
    val post: String = self.split(":")(1)
    def \:(x: XMLNode): XMLNodeSeq = (x \ post).filter(_.prefix == pre)
    def \:(x: XMLNodeSeq): XMLNodeSeq = (x \ post).filter(_.prefix == pre)
  }

  def findKeyById[T](keys: Seq[Key[_]], id: String): Option[Key[T]] = {
    keys.find(_.id == id).map(_.asInstanceOf[Key[T]])
  }

  def findKeyByName[T](keys: Seq[Key[_]], keyFor: String, name: String): Option[Key[T]] = {
    keys.find(k => (k.keyFor == keyFor && k.name == Some(name))).map(_.asInstanceOf[Key[T]])
  }

  def findNode(nodes: Seq[Node], id: String): Option[Node] = {
    val rootMatch = nodes.find(_.id == id)

    if (rootMatch.isDefined) rootMatch
    else {
      nodes.flatMap(_.subgraph).flatMap{g => findNode(g.nodes, id)}.headOption
    }
  }

  def parseKey(x: XMLNode)(implicit loc: ParserLocation): Key[_] = {
    val id = x \@ "id"
    val keyFor = x \@ "for"
    val attrName = (x \ "@attr.name").headOption.map(_.text)

    val default = (x \ "default").headOption

    (x \ "@attr.type").headOption.map(_.text) match {
      case Some("string") => StringKey(id, keyFor, attrName, default.map(_.text))
      case Some("int") => IntKey(id, keyFor, attrName, default.map(_.text.toInt))
      case Some(unknown) => throw new IllegalArgumentException("unknown key-Element: " + unknown + " " + loc)
      case None => {
        (x \ "@yfiles.type").headOption.map(_.text) match {
          case yfilesType @ Some("resources")    => NothingKey(id, keyFor, yfilesType)

          case yfilesType @ Some("portgraphics") => NothingKey(id, keyFor, yfilesType)
          case yfilesType @ Some("portgeometry") => NothingKey(id, keyFor, yfilesType)
          case yfilesType @ Some("portuserdata") => NothingKey(id, keyFor, yfilesType)

          case yfilesType @ Some("nodegraphics") => YNodeGraphicsKey(id, keyFor, yfilesType)
          case yfilesType @ Some("edgegraphics") => YEdgeGraphicsKey(id, keyFor, yfilesType)
          case Some(unknown) => throw new IllegalArgumentException("unknown yfiles.type attribute: " + unknown)
          case None => throw new IllegalArgumentException("a key-Element must have a yfiles.type attribute " + loc)
        }
      }
    }
  }

  def parseYBorderStyle(x: XMLNode): YBorderStyle = {
    val color = x \@ "color"
    val typ = x \@ "type"
    val width = (x \@ "width").toDouble
    YBorderStyle(color, typ, width)
  }

  def parseYNodeLabel(x: XMLNode): YNodeLabel = {
    //val modelName = x \@ "modelName"
    YNodeLabel(x.text.trim)
  }

  def parseYEdgeLabel(x: XMLNode): YEdgeLabel = {
    val iconDataString = (x \@ "iconData")
    val iconData =
      if (iconDataString == "") None
      else Some(iconDataString.toInt)

    YEdgeLabel(x.text.trim, iconData)
  }

  def parseYLineStyle(x: XMLNode): YLineStyle = {
    val color: String = (x \@ "color")
    val typ: String = (x \@ "type")
    val width: Double = (x \@ "width").toDouble

    YLineStyle(RGBColor(color), typ, width)
  }

  def parseYStyleProperties(x: XMLNode): Seq[YProperty] = {
    (x \: "y:Property") map parseYProperty
  }

  def parseYProperty(x: XMLNode): YProperty = {
    val clazz = x \@ "class"
    val name = x \@ "name"
    val value = x \@ "value"
    YProperty(clazz, name, value)
  }

  def parseYProxyAutoBoundsNode(x: XMLNode)(implicit loc: ParserLocation): YProxyAutoBoundsNode = {
    val groupNodes1: Seq[YGroupNode] = ((x \: "y:Realizers") \: "y:GroupNode") map parseYGroupNode
    val groupNodes2: Seq[YGroupNode] = ((x \: "y:Realizers") \: "y:GenericGroupNode") map parseYGroupNode

    YProxyAutoBoundsNode(groupNodes1 ++ groupNodes2)
  }

  def hasNoText(x: XMLNode): Boolean = {
    val hasText: String = x \@ "hasText"
    (hasText == "false")
  }

  def parseYGroupNode(x: XMLNode)(implicit loc: ParserLocation): YGroupNode = {
    val label: Seq[YNodeLabel] = (x \: "y:NodeLabel") filterNot hasNoText map parseYNodeLabel
    val borderStyle: Option[YBorderStyle] = (x \: "y:BorderStyle").headOption map parseYBorderStyle
    val styleProperties: Option[Seq[YProperty]] = (x \: "y:StyleProperties").headOption map parseYStyleProperties

    // FIXME: ugly to handle "M" here

    val label2: Set[YNodeLabel] = label.filterNot(_.text == "M").toSet

    if (label2.size > 1) throw new Exception("Expected max one label, got: " + label2 + ". xml: " + x + " " + loc)

    YGroupNode(label.headOption, borderStyle, styleProperties.getOrElse(Seq()))
  }

  def parseYGenericNode(x: XMLNode)(implicit loc: ParserLocation): YGenericNode = {
    val configuration: Option[String] = (x \ "@configuration").headOption.map(_.text)
    val borderStyle: Option[YBorderStyle] = (x \: "y:BorderStyle").headOption map parseYBorderStyle
    val label: Seq[YNodeLabel] = (x \: "y:NodeLabel") filterNot hasNoText map parseYNodeLabel
    val styleProperties: Option[Seq[YProperty]] = (x \: "y:StyleProperties").headOption map parseYStyleProperties

    if (label.size > 1) throw new Exception("Expected max one label, got: " + label + ". xml: " + x + " " + loc)

    YGenericNode(configuration, borderStyle, label.headOption, styleProperties.getOrElse(Seq()))
  }

  def parseYGenericEdge(x: XMLNode)(implicit loc: ParserLocation): YGenericEdge = {
    val configuration: Option[String] = (x \ "@configuration").headOption.map(_.text)
    val lineStyle: Option[YLineStyle] = ((x \: "y:LineStyle") map parseYLineStyle).headOption
    val label: Seq[YEdgeLabel] = (x \: "y:EdgeLabel") filterNot hasNoText map parseYEdgeLabel
    val styleProperties: Option[Seq[YProperty]] = (x \: "y:StyleProperties").headOption map parseYStyleProperties

    if (label.size > 1) throw new Exception("Expected max one label, got: " + label + ". xml: " + x + " " + loc)

    YGenericEdge(configuration, lineStyle, label.headOption, styleProperties.getOrElse(Seq()))
  }

  def parseData(x: XMLNode, keys: Seq[Key[_]])(implicit loc: ParserLocation): Data[_] = {
    val keyId = x \@ "key"
    val key: Key[_] = Helper.findKeyById(keys, keyId).get

    parseData(x, key)
  }

  def parseData[T](x: XMLNode, key: Key[T])(implicit loc: ParserLocation): Data[T] = {
    key match {
      case k: IntKey  => parseDataInt(x, key)
      case k: StringKey  => parseDataString(x, key)
      case k: YNodeGraphicsKey  => parseDataYNodeGraphics(x, key)
      case k: YEdgeGraphicsKey  => parseDataYEdgeGraphics(x, key)
      case k: NothingKey => parseDataNothing(x, key)
    }
  }

  private def parseDataInt(x: XMLNode, key: Key[Int])(implicit loc: ParserLocation): Data[Int] = {
    val value: Option[Int] = if (x.child.isEmpty) None else Some(x.text.trim.toInt)
    IntData(key, value)
  }

  private def parseDataString(x: XMLNode, key: Key[String])(implicit loc: ParserLocation): Data[String] = {
    val value: Option[String] = if (x.child.isEmpty) None else Some(x.text.trim)
    StringData(key, value)
  }

  private def parseDataYNodeGraphics(x: XMLNode, key: Key[YNodeGraphics])(implicit loc: ParserLocation): Data[YNodeGraphics] = {
    val value: Option[YNodeGraphics] = if (x.child.isEmpty) None else {
      val yNodeO = (x \: "y:GenericNode").headOption

      if (yNodeO.isDefined) {
        val yNode: YGenericNode = parseYGenericNode(yNodeO.get)
        Some(yNode.asInstanceOf[YNodeGraphics]) // unproblematic upcast
      }
      else {
        val yProxyO = (x \: "y:ProxyAutoBoundsNode").headOption

        yProxyO.map(parseYProxyAutoBoundsNode)
      }
    }
    YNodeGraphicsData(key, value)
  }

  private def parseDataYEdgeGraphics(x: XMLNode, key: Key[YEdgeGraphics])(implicit loc: ParserLocation): Data[YEdgeGraphics] = {
    val value: Option[YGenericEdge] = if (x.child.isEmpty) None else {
      val yEdgeO = (x \: "y:GenericEdge").headOption

      yEdgeO.map(parseYGenericEdge)
    }
    YEdgeGraphicsData(key, value)
  }

  private def parseDataNothing(x: XMLNode, key: Key[Nothing])(implicit loc: ParserLocation): Data[Nothing] = {
    val value: Option[Nothing] = None
    NothingData(key, value)
  }

  def findData[T](data: Seq[Data[_]], dataKeyType: String, name: String): Option[Data[T]] = {
    data.find(x => (x.key.keyFor == dataKeyType && x.key.name == Some(name))).flatMap(x => asInstanceOfOption[Data[T]](x))
  }

  def asInstanceOfOption[T: ClassTag](o: Any): Option[T] = Some(o) collect { case m: T => m }

  def parseNode(x: XMLNode, keys: Seq[Key[_]])(implicit loc: ParserLocation): Node = {
    val id = x \@ "id"

    val yFoldertype = (x \ "@yfiles.foldertype").headOption.map(_.text)
    val data = x \ "data" map {x => parseData(x, keys) }
    val subgraphs = x \ "graph" map {x => parseGraph(x, keys) }
    if (subgraphs.size > 1) throw new Exception("id: " + id + ". Expected max one subgraph. subgraphs: " + subgraphs + " " + loc)
    val subgraph: Option[Graph] = subgraphs.headOption

    val description = findData[String](data, "node", "description").flatMap(_.value)

    val nodeGraphics: Option[YNodeGraphics] = findData[YNodeGraphics](data, "node", "nodegraphics").flatMap(_.value)
    val genericNode: Option[YGenericNode] = nodeGraphics.flatMap(asInstanceOfOption[YGenericNode])
    val groupNodes: Seq[YGroupNode] = nodeGraphics.flatMap(asInstanceOfOption[YProxyAutoBoundsNode]).map(_.groupNodes).getOrElse(Seq())

    val (bpmnType, taskType) = if (genericNode.isDefined) {
      val bpmnType = genericNode.flatMap(_.styleProperties.find(_.clazz == "com.yworks.yfiles.bpmn.view.BPMNTypeEnum").map(_.value))
      val taskType = genericNode.flatMap(_.styleProperties.find(_.clazz == "com.yworks.yfiles.bpmn.view.TaskTypeEnum").map(_.value))
      (bpmnType, taskType)
    }
    else if (groupNodes.nonEmpty) {
      val bpmnTypes: Set[String] = groupNodes.flatMap(_.styleProperties.find(_.clazz == "com.yworks.yfiles.bpmn.view.BPMNTypeEnum").map(_.value)).toSet
      val taskTypes: Set[String] = groupNodes.flatMap(_.styleProperties.find(_.clazz == "com.yworks.yfiles.bpmn.view.TaskTypeEnum").map(_.value)).toSet

      if (bpmnTypes.size > 1) {
        throw new Exception("id: " + id + ". Expected max one bpmnType. bpmnTypes: " + bpmnTypes + " " + loc)
      }
      if (taskTypes.size > 1) {
        throw new Exception("id: " + id + ". Expected max one taskType. taskTypes: " + taskTypes + " " + loc)
      }

      (bpmnTypes.headOption, taskTypes.headOption)
    }
    else {
      (None, None)
    }

    val labels: Set[YNodeLabel] = (groupNodes.flatMap(_.label) ++ genericNode.flatMap(_.label)).toSet

    if (labels.size > 1) throw new Exception("id: " + id + ". Expected max one label. labels: " + labels + " " + loc)

    Node(id, description, data, subgraph, yFoldertype, genericNode, groupNodes, labels.headOption, bpmnType, taskType)
  }

  def parseEdge(x: XMLNode, keys: Seq[Key[_]], nodes: Seq[Node])(implicit loc: ParserLocation): Edge = {
    val id = x \@ "id"
    val data = x \ "data" map {x => parseData(x, keys) }

    val description = findData[String](data, "edge", "description").flatMap(_.value)

    val genericEdge: Option[YGenericEdge] = findData[YEdgeGraphics](data, "edge", "edgegraphics").flatMap(_.value).flatMap(asInstanceOfOption[YGenericEdge])
    val bpmnType = genericEdge.flatMap(_.styleProperties.find(_.clazz == "com.yworks.yfiles.bpmn.view.BPMNTypeEnum")).map(_.value)

    val lineColor: Option[RGBColor] = genericEdge.flatMap(_.lineStyle.map(_.color))
    val lineType: Option[String] = genericEdge.flatMap(_.lineStyle.map(_.typ))
    val label: Option[YEdgeLabel] = genericEdge.flatMap(_.label)

    val sourceId = x \@ "source"
    val targetId = x \@ "target"

    val source = Helper.findNode(nodes, sourceId)
    val target = Helper.findNode(nodes, targetId)

    if (source.isEmpty) throw new Exception("Can not find source '" + sourceId +"' of edge '" + id  + "' " + loc)
    if (target.isEmpty) throw new Exception("Can not find target '" + targetId +"' of edge '" + id  + "' " + loc)

    Edge(id, description, source.get.reference, target.get.reference, genericEdge, lineColor, lineType, label, data, bpmnType)
  }

  def parseGraph(x: XMLNode, keys: Seq[Key[_]])(implicit loc: ParserLocation): Graph = {
    val id = x \@ "id"
    val edgedefault = x \@ "edgedefault"
    val data = x \ "data" map {x => parseData(x, keys) }
    val nodes = x \ "node" map {x => parseNode(x, keys) }
    val edges = x \ "edge" map {x => parseEdge(x, keys, nodes) }

    Graph(id, edgedefault, data, nodes, edges)
  }

  def parseGraphML(x: XMLNode)(implicit loc: ParserLocation): GraphML = {
    logger.trace("parseGraphML: {}", x)

    val keys = x \ "key" map parseKey
    val data = x \ "data" map {x => parseData(x, keys) }
    val graph = (x \ "graph" map {x => parseGraph(x, keys) }).head
    GraphML(keys, data, graph)
  }

  private def setToMapOfSets[A, B](x: Set[(A, B)]): Map[A, Set[B]] = {
    x.groupBy(_._1).mapValues(_.map(_._2)).toMap
  }

  implicit class richSeqSet[A, B](self: Set[(A, B)]) {
    def toSetMap: Map[A, Set[B]] = setToMapOfSets(self)
  }
}
