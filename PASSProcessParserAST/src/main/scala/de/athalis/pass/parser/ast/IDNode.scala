package de.athalis.pass.parser.ast

import org.jparsec.Token

object IDNode {
  val PARSER: java.util.function.Function[Token, MapAbleNode[String]] = (from) => IDNode(from.toString)
}

case class IDNode(value: String) extends CustomNode with MapAbleNode[String]
