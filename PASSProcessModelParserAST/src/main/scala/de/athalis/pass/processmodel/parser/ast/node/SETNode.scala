package de.athalis.pass.processmodel.parser.ast.node

object SETNode {
  def PARSER[T]: java.util.function.Function[Set[MapAbleNode[T]], MapAbleNode[Set[T]]] = (from) => SETNode[T](from.map(_.value))
}

case class SETNode[T](value: Set[T]) extends CustomNode with MapAbleNode[Set[T]]
