package de.athalis.pass.processmodel.parser.ast.node

import org.jparsec.Token

object KWNode {
  val PARSER: java.util.function.Function[Token, KWNode] = (from) => KWNode(from.toString)
}

case class KWNode(value: String) extends CustomNode with MapAbleNode[String]
