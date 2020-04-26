package de.athalis.pass.parser.ast

import org.jparsec.Token
import org.slf4j.LoggerFactory

object NodeDebugger {
  private val logger = LoggerFactory.getLogger("NodeDebugger")

  def trace(from: Any, prefix: String = ""): Unit = from match {
    case col: Seq[_]   => for (i <- col.indices) trace(col(i), prefix + "[" + i + "]")
    case a: Array[_]   => trace(a.toSeq, prefix)
    case col: Set[_]   => trace(col.toSeq, prefix)
    case p: (_, _) => {
      trace(p._1, prefix + ".a")
      trace(p._2, prefix + ".b")
    }
    case None => logger.trace(prefix + ": None")
    case Some(value) => trace(value, prefix)

    case null          => logger.trace(prefix + ": NULL")
    case n: CustomNode => logger.trace(prefix + ": " + n)
    case t: Token      => logger.trace(prefix + ": " + t)

    case x => throw new IllegalArgumentException("unexpected: " + x + " (" + x.getClass + ")")
  }
}
