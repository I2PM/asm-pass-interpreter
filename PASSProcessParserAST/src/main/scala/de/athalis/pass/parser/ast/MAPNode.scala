package de.athalis.pass.parser.ast

object MAPNode {
  def empty[K, V] = MAPNode[K, V](Map.empty[K, V])

  val PARSERAny = PARSER[Any, Any]

  private def PARSER[A, B]: java.util.function.Function[Seq[(MapAbleNode[A], MapAbleNode[B])], MapAbleNode[Map[A, B]]] = (from) => {
    MAPNode[A, B](from.map(x => (x._1.value -> x._2.value)).toMap)
  }

  val PARSEREmpty: java.util.function.Function[MapAbleNode[String], MapAbleNode[Map[Any, Nothing]]] = (from) => {
    if (from.value != "->") throw new IllegalArgumentException("expected '->' to construct empty MAPNode")
    val m = Map.empty[Any, Nothing]
    MAPNode[Any, Nothing](m)
  }
}

case class MAPNode[A, +B](value: Map[A, B]) extends CustomNode with MapAbleNode[Map[A, B]]
