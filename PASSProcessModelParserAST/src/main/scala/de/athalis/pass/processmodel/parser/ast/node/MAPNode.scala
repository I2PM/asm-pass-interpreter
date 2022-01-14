package de.athalis.pass.processmodel.parser.ast.node

object MAPNode {
  val emptyMap: MAPNode[Any, Nothing] = MAPNode[Any, Nothing](Map.empty[Any, Nothing])

  def empty[K, V]: MAPNode[K, V] = emptyMap.asInstanceOf[MAPNode[K, V]]

  def PARSER[K, V](): java.util.function.Function[Seq[(MapAbleNode[K], MapAbleNode[V])], MapAbleNode[Map[K, V]]] = (from) => {
    MAPNode[K, V](from.map(pair => (pair._1.value -> pair._2.value)).toMap)
  }

  val PARSERAny: java.util.function.Function[Seq[(MapAbleNode[Any], MapAbleNode[Any])], MapAbleNode[Map[Any, Any]]] = PARSER[Any, Any]()

  def PARSEREmpty[K, V](): java.util.function.Function[MapAbleNode[String], MapAbleNode[Map[K, V]]] = (from) => {
    if (from.value != "->") throw new IllegalArgumentException("expected '->' to construct empty MAPNode")
    MAPNode.empty[K, V]
  }

  val PARSEREmptyAny: java.util.function.Function[MapAbleNode[String], MapAbleNode[Map[Any, Nothing]]] = PARSEREmpty[Any, Nothing]()
}

case class MAPNode[K, +V](value: Map[K, V]) extends CustomNode with MapAbleNode[Map[K, V]]
