package de.athalis.pass.processmodel.parser.ast.node.pass

import de.athalis.pass.processmodel.parser.ast.node.CustomNode
import de.athalis.pass.processmodel.parser.ast.node.MapAbleNode

import org.slf4j.LoggerFactory

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
