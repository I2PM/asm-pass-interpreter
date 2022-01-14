package de.athalis.pass.processmodel.parser.ast.node

object SEQNode {
  def PARSER[T]: java.util.function.Function[Seq[MapAbleNode[T]], MapAbleNode[Seq[T]]] = (from) => SEQNode[T](from.map(_.value))
}

case class SEQNode[+T](value: Seq[T]) extends CustomNode with MapAbleNode[Seq[T]]
