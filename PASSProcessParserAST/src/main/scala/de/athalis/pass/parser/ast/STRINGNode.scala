package de.athalis.pass.parser.ast

object STRINGNode {
  val PARSER: java.util.function.Function[String, MapAbleNode[String]] = (from) => STRINGNode(from)
}

case class STRINGNode(value: String) extends CustomNode with MapAbleNode[String]
