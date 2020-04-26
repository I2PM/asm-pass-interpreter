package de.athalis.pass.parser.ast

import org.jparsec.Token

object KWNode {
  val PARSER: java.util.function.Function[Token, KWNode] = (from) => KWNode(from.toString)
}

case class KWNode(value: String) extends CustomNode with MapAbleNode[String]
