package de.athalis.pass.processmodel.parser.graphml.structure

import de.athalis.pass.processmodel.parser.graphml.Helper

import scala.collection.immutable._
import scala.collection.{Seq => DefaultSeq}

sealed trait Key[T] {
  def id: String
  def keyFor: String
  def name: Option[String]
  def default: Option[T]
}
case class NothingKey      (id: String, keyFor: String, name: Option[String] = None, default: Option[Nothing] = None) extends Key[Nothing]
case class StringKey       (id: String, keyFor: String, name: Option[String] = None, default: Option[String] = None) extends Key[String]
case class IntKey          (id: String, keyFor: String, name: Option[String] = None, default: Option[Int] = None) extends Key[Int]
case class YNodeGraphicsKey(id: String, keyFor: String, name: Option[String] = None, default: Option[YNodeGraphics] = None) extends Key[YNodeGraphics]
case class YEdgeGraphicsKey(id: String, keyFor: String, name: Option[String] = None, default: Option[YEdgeGraphics] = None) extends Key[YEdgeGraphics]

sealed trait Data[T]{
  def key: Key[T]
  def value: Option[T]
}
case class NothingData(key: Key[Nothing], value: Option[Nothing]) extends Data[Nothing]
case class StringData(key: Key[String], value: Option[String]) extends Data[String]
case class IntData(key: Key[Int], value: Option[Int]) extends Data[Int]
case class YNodeGraphicsData(key: Key[YNodeGraphics], value: Option[YNodeGraphics]) extends Data[YNodeGraphics]
case class YEdgeGraphicsData(key: Key[YEdgeGraphics], value: Option[YEdgeGraphics]) extends Data[YEdgeGraphics]

// TODO: only used for "IPKeyName" which can now be expressed otherwise. Remove this completely?
sealed trait HasData {
  def dataKeyType: String
  def data: Seq[Data[_]]
  def getData[T](name: String): Option[Data[T]] = Helper.findData[T](data, dataKeyType, name)
}

case class GraphML(keys: Seq[Key[_]], data: Seq[Data[_]], graph: Graph) extends HasData {
  override def dataKeyType: String = "graphml"

  def findKeyById[T](id: String): Option[Key[T]] = Helper.findKeyById[T](keys, id)
  def findKeyByName[T](keyFor: String, name: String): Option[Key[T]] = Helper.findKeyByName[T](keys, keyFor, name)
}

case class Graph(id: String, edgedefault: String, data: Seq[Data[_]], nodes: Seq[Node], edges: Seq[Edge]) extends HasData {
  def dataKeyType = "graph"
}

trait HasLabel {
  def label: Option[Label]
  def getLabel: Option[String] = label.map(_.remaining)
}

case class NodeReference(id: String)

case class Node(
    id: String,
    description: Option[String],
    data: Seq[Data[_]],
    subgraph: Option[Graph],
    yFoldertype: Option[String],
    genericNode: Option[YGenericNode],
    groupNodes: Seq[YGroupNode],
    label: Option[YNodeLabel],
    BPMNType: Option[String],
    BPMNTaskType: Option[String]
  ) extends HasData with HasLabel {
  def reference: NodeReference = NodeReference(id)
  override def dataKeyType: String = "node"
  def mkString: String = "Node("+id+",..)"
}

case class Edge(
    id: String,
    description: Option[String],
    source: NodeReference,
    target: NodeReference,
    genericEdge: Option[YGenericEdge],
    lineColor: Option[RGBColor],
    lineType: Option[String],
    label: Option[YEdgeLabel],
    data: Seq[Data[_]],
    BPMNType: Option[String]
  ) extends HasData with HasLabel {
  override def dataKeyType: String = "edge"
  def mkString: String = "Edge("+id+", "+source.id+", "+target.id+",..)"
}
//case object Port


sealed trait YNodeGraphics
sealed trait YEdgeGraphics

case class YProxyAutoBoundsNode(groupNodes: Seq[YGroupNode]) extends YNodeGraphics

case class YGroupNode(
    label: Option[YNodeLabel],
    borderStyle: Option[YBorderStyle],
    styleProperties: Seq[YProperty]
  ) extends HasLabel

case class YGenericNode(
    configuration: Option[String],
    borderStyle: Option[YBorderStyle],
    label: Option[YNodeLabel],
    styleProperties: Seq[YProperty]
  ) extends YNodeGraphics with HasLabel

case class YGenericEdge(
    configuration: Option[String],
    lineStyle: Option[YLineStyle],
    label: Option[YEdgeLabel],
    styleProperties: Seq[YProperty]
  ) extends YEdgeGraphics with HasLabel

case class YLineStyle(color: RGBColor, typ: String, width: Double)

case class RGBColor(red: Int, green: Int, blue: Int, alpha: Int = 255) {
  def mkString: String = {
    // FIXME: alpha
    "#" + String.format("%02X", Integer.valueOf(red)) + String.format("%02X", Integer.valueOf(green)) + String.format("%02X", Integer.valueOf(blue))
  }
}
object RGBColor {
  def apply(from: String): RGBColor = {
    if (!from.startsWith("#") || (from.length != 7 /*&& from.length != 9*/)) {
      throw new IllegalArgumentException("unexpected scheme:" + from)
    }

    val red: Int   = Integer.valueOf(from.substring(1, 3), 16)
    val green: Int = Integer.valueOf(from.substring(3, 5), 16)
    val blue: Int  = Integer.valueOf(from.substring(5, 7), 16)

    RGBColor(red, green, blue)
  }

  val black = RGBColor(0, 0, 0)
  val red   = RGBColor(255, 0, 0)
  val green = RGBColor(0, 255, 0)
  val blue  = RGBColor(0, 0, 255)
}

sealed trait Label {
  def text: String
  private val data = LabelData(text)

  def apply(key: String): Option[String] = data.data.get(key)
  val remaining = data.remaining
}


case class LabelData(data: Map[String, String], remaining: String)

object LabelData {
  def apply(text: String): LabelData = {
    val lines: DefaultSeq[String] = text.split("\n")
    val x = lines.filter(_.contains(":"))

    val m: Map[String, String] = x.map(_.split(":")).map(
      x => (x.head, x.tail.mkString(":"))
    ).toMap

    // TODO: very ugly
    val remaining = lines.filterNot(l => (l.startsWith("ID:") || l.startsWith("PRIO:"))).mkString("\n")

    LabelData(m, remaining)
  }
}

case class YNodeLabel(text: String) extends Label
case class YEdgeLabel(text: String, iconData: Option[Int]) extends Label
case class YBorderStyle(color: String, typ: String, width: Double)
case class YProperty(clazz: String, name: String, value: String)
