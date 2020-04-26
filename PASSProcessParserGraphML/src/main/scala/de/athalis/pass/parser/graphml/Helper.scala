package de.athalis.pass.parser.graphml

import org.slf4j.LoggerFactory

import scala.reflect._
import scala.reflect.runtime.universe._
import scala.xml.{Node => XMLNode, NodeSeq => XMLNodeSeq}
import scala.collection.immutable._

import de.athalis.pass.parser.graphml.structure._

object Helper {
  private val logger = LoggerFactory.getLogger(Helper.getClass)

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

  def parseKey(x: XMLNode): Key[_] = {
    val id = x \@ "id"
    val keyFor = x \@ "for"
    val attrName = (x \ "@attr.name").headOption.map(_.text)

    val default = (x \ "default").headOption

    (x \ "@attr.type").headOption.map(_.text) match {
      case Some("string") => StringKey(id, keyFor, attrName, default.map(_.text))
      case Some("int") => IntKey(id, keyFor, attrName, default.map(_.text.toInt))
      case Some(unknown) => throw new IllegalArgumentException("unknown key-Element: " + unknown)
      case None => {
        (x \ "@yfiles.type").headOption.map(_.text) match {
          case yfilesType @ Some("resources")    => NothingKey(id, keyFor, yfilesType)

          case yfilesType @ Some("portgraphics") => NothingKey(id, keyFor, yfilesType)
          case yfilesType @ Some("portgeometry") => NothingKey(id, keyFor, yfilesType)
          case yfilesType @ Some("portuserdata") => NothingKey(id, keyFor, yfilesType)

          case yfilesType @ Some("nodegraphics") => YNodeGraphicsKey(id, keyFor, yfilesType)
          case yfilesType @ Some("edgegraphics") => YEdgeGraphicsKey(id, keyFor, yfilesType)
          case Some(unknown) => throw new IllegalArgumentException("unknown yfiles.type attribute: " + unknown)
          case None => throw new IllegalArgumentException("a key-Element must have a yfiles.type attribute")
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

  def parseYProxyAutoBoundsNode(x: XMLNode): YProxyAutoBoundsNode = {
    val groupNodes1: Seq[YGroupNode] = ((x \: "y:Realizers") \: "y:GroupNode") map parseYGroupNode
    val groupNodes2: Seq[YGroupNode] = ((x \: "y:Realizers") \: "y:GenericGroupNode") map parseYGroupNode

    YProxyAutoBoundsNode(groupNodes1 ++ groupNodes2)
  }

  def hasNoText(x: XMLNode): Boolean = {
    val hasText: String = x \@ "hasText"
    (hasText == "false")
  }

  def parseYGroupNode(x: XMLNode): YGroupNode = {
    val label: Seq[YNodeLabel] = (x \: "y:NodeLabel") filterNot hasNoText map parseYNodeLabel
    val borderStyle: Option[YBorderStyle] = (x \: "y:BorderStyle").headOption map parseYBorderStyle
    val styleProperties: Option[Seq[YProperty]] = (x \: "y:StyleProperties").headOption map parseYStyleProperties

    // FIXME: ugly to handle "M" here

    val label2: Set[YNodeLabel] = label.filterNot(_.text == "M").toSet

    if (label2.size > 1) throw new Exception("Expected max one label, got: " + label2 + ". xml: " + x)

    YGroupNode(label.headOption, borderStyle, styleProperties.getOrElse(Seq()))
  }

  def parseYGenericNode(x: XMLNode): YGenericNode = {
    val configuration: Option[String] = (x \ "@configuration").headOption.map(_.text)
    val borderStyle: Option[YBorderStyle] = (x \: "y:BorderStyle").headOption map parseYBorderStyle
    val label: Seq[YNodeLabel] = (x \: "y:NodeLabel") filterNot hasNoText map parseYNodeLabel
    val styleProperties: Option[Seq[YProperty]] = (x \: "y:StyleProperties").headOption map parseYStyleProperties

    if (label.size > 1) throw new Exception("Expected max one label, got: " + label + ". xml: " + x)

    YGenericNode(configuration, borderStyle, label.headOption, styleProperties.getOrElse(Seq()))
  }

  def parseYGenericEdge(x: XMLNode): YGenericEdge = {
    val configuration: Option[String] = (x \ "@configuration").headOption.map(_.text)
    val lineStyle: Option[YLineStyle] = ((x \: "y:LineStyle") map parseYLineStyle).headOption
    val label: Seq[YEdgeLabel] = (x \: "y:EdgeLabel") filterNot hasNoText map parseYEdgeLabel
    val styleProperties: Option[Seq[YProperty]] = (x \: "y:StyleProperties").headOption map parseYStyleProperties

    if (label.size > 1) throw new Exception("Expected max one label, got: " + label + ". xml: " + x)

    YGenericEdge(configuration, lineStyle, label.headOption, styleProperties.getOrElse(Seq()))
  }

  def parseData(x: XMLNode, keys: Seq[Key[_]]): Data[_] = {
    val keyId = x \@ "key"
    val key: Key[_] = Helper.findKeyById(keys, keyId).get

    parseData(x, key)
  }

  // TODO: may split into parseDataInt etc to avoid weird typeTags
  def parseData[T](x: XMLNode, key: Key[T])(implicit tag: TypeTag[T]): Data[T] = {
    val value: Option[T] = if (x.child.isEmpty) None else key match {
      case k: IntKey  => Some(x.text.trim.toInt)
      case k: StringKey  => Some(x.text.trim)
      case k: YNodeGraphicsKey  => {
        val yNodeO = (x \: "y:GenericNode").headOption

        if (yNodeO.isDefined) {
          val yNode: YGenericNode = parseYGenericNode(yNodeO.get)
          Some(yNode.asInstanceOf[T])
        }
        else {
          val yProxyO = (x \: "y:ProxyAutoBoundsNode").headOption

          yProxyO.map(parseYProxyAutoBoundsNode)
        }
      }
      case k: YEdgeGraphicsKey  => {
        val yEdgeO = (x \: "y:GenericEdge").headOption

        yEdgeO.map(parseYGenericEdge)
      }
      case k: NothingKey => {
        logger.warn("not implemented data-key '{}', id = {}, for {}", k.name, k.id, k.keyFor)
        None
      }
    }

    key match {
      case k: IntKey  => IntData(k, value)
      case k: StringKey  => StringData(k, value)
      case k: YNodeGraphicsKey  => YNodeGraphicsData(k, value)
      case k: YEdgeGraphicsKey  => YEdgeGraphicsData(k, value)
      case k: NothingKey => NothingData(k, value)
    }
  }

  def findData[T](data: Seq[Data[_]], dataKeyType: String, name: String): Option[Data[T]] = {
    data.find(x => (x.key.keyFor == dataKeyType && x.key.name == Some(name))).flatMap(x => asInstanceOfOption[Data[T]](x))
  }

  def asInstanceOfOption[T: ClassTag](o: Any): Option[T] = Some(o) collect { case m: T => m }

  def parseNode(x: XMLNode, keys: Seq[Key[_]]): Node = {
    val id = x \@ "id"

    val yFoldertype = (x \ "@yfiles.foldertype").headOption.map(_.text)
    val data = x \ "data" map {x => parseData(x, keys) }
    val subgraphs = x \ "graph" map {x => parseGraph(x, keys) }
    if (subgraphs.size > 1) throw new Exception("id: " + id + ". Expected max one subgraph. subgraphs: " + subgraphs)
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
        throw new Exception("id: " + id + ". Expected max one bpmnType. bpmnTypes: " + bpmnTypes)
      }
      if (taskTypes.size > 1) {
        throw new Exception("id: " + id + ". Expected max one taskType. taskTypes: " + taskTypes)
      }

      (bpmnTypes.headOption, taskTypes.headOption)
    }
    else {
      (None, None)
    }

    var labels: Set[YNodeLabel] = (groupNodes.flatMap(_.label) ++ genericNode.flatMap(_.label)).toSet

    if (labels.size > 1) throw new Exception("id: " + id + ". Expected max one label. labels: " + labels)

    Node(id, description, data, subgraph, yFoldertype, genericNode, groupNodes, labels.headOption, bpmnType, taskType)
  }

  def parseEdge(x: XMLNode, keys: Seq[Key[_]], nodes: Seq[Node]): Edge = {
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

    if (source.isEmpty) throw new Exception("Can not find source '" + sourceId +"' of edge '" + id  + "'")
    if (target.isEmpty) throw new Exception("Can not find target '" + targetId +"' of edge '" + id  + "'")

    Edge(id, description, source.get.reference, target.get.reference, genericEdge, lineColor, lineType, label, data, bpmnType)
  }

  def parseGraph(x: XMLNode, keys: Seq[Key[_]]): Graph = {
    val id = x \@ "id"
    val edgedefault = x \@ "edgedefault"
    val data = x \ "data" map {x => parseData(x, keys) }
    val nodes = x \ "node" map {x => parseNode(x, keys) }
    val edges = x \ "edge" map {x => parseEdge(x, keys, nodes) }

    Graph(id, edgedefault, data, nodes, edges)
  }

  def parseGraphML(x: XMLNode): GraphML = {
    logger.trace("parseGraphML: {}", x)

    val keys = x \ "key" map parseKey
    val data = x \ "data" map {x => parseData(x, keys) }
    val graph = (x \ "graph" map {x => parseGraph(x, keys) }).head
    GraphML(keys, data, graph)
  }

  def setToMapOfSets[A, B](x: Set[(A, B)]): Map[A, Set[B]] = {
    x.groupBy(_._1).mapValues(_.map(_._2))
  }

  implicit class richSeqSet[A, B](self: Set[(A, B)]) {
    def toSetMap: Map[A, Set[B]] = setToMapOfSets(self)
  }
}