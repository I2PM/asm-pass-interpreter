package de.athalis.pass.parser.ast

object NUMBERNode {
  val PARSER: java.util.function.Function[String, MapAbleNode[Int]] = (from) => NUMBERNode(from.toInt)
}

case class NUMBERNode(value: Int) extends CustomNode with MapAbleNode[Int]
