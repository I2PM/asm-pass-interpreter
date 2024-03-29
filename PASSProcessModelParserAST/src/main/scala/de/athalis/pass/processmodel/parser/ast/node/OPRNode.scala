package de.athalis.pass.processmodel.parser.ast.node

import de.athalis.pass.processmodel.parser.ast.PASSParser

import org.jparsec.Token

object OPRNode {
  val PARSER: java.util.function.Function[Token, MapAbleNode[String]] = (from) => OPRNode(from.toString)
}

case class OPRNode(value: String) extends CustomNode with MapAbleNode[String]

object OPRNodeInt {
  val PARSER: java.util.function.Function[MapAbleNode[String], MapAbleNode[Int]] = (from) => from.value match {
    case PASSParser.OPR_ALL => OPRNodeInt(0)
    case x => throw new IllegalArgumentException("only OPR_ANY can be cast to Int, found: " + x)
  }
}

case class OPRNodeInt(value: Int) extends CustomNode with MapAbleNode[Int]
