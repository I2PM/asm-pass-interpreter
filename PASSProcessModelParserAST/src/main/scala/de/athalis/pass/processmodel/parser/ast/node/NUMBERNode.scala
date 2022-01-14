package de.athalis.pass.processmodel.parser.ast.node

object NUMBERNode {
  val PARSER: java.util.function.Function[String, MapAbleNode[Int]] = (from) => NUMBERNode(from.toInt)
}

case class NUMBERNode(value: Int) extends CustomNode with MapAbleNode[Int]
