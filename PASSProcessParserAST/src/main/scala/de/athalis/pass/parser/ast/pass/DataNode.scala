package de.athalis.pass.parser.ast.pass

import org.slf4j.LoggerFactory

import de.athalis.pass.parser.ast._

object DataNode {
  private val logger = LoggerFactory.getLogger(DataNode.getClass)

  val PARSER: java.util.function.Function[MapAbleNode[Map[Any, Any]], DataNode] = (from) => {
    logger.trace("DataNode create")

    val m = from.value.map {
      case (a: String, b: Any) => (a -> b)
      case _ => throw new IllegalArgumentException("expected Map[String, Any]")
    }

    new DataNode(m)
  }
}

class DataNode(val value: Map[String, Any]) extends CustomNode
