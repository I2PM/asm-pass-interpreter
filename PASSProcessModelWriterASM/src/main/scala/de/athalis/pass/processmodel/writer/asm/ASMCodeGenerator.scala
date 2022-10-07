package de.athalis.pass.processmodel.writer.asm

import de.athalis.pass.processmodel.writer.asm.TUDarmstadtModel2ASMMap.ProcessModelASMMap

import org.slf4j.LoggerFactory

import java.lang.System.{lineSeparator => EOL}

import scala.collection.immutable.TreeMap
import scala.collection.immutable.TreeSet

object ASMCodeGenerator {
  private val logger = LoggerFactory.getLogger(ASMCodeGenerator.getClass)

  def toASMRule(ruleName: String, processModelID: String, processModelMap: ProcessModelASMMap): String = {
    logger.debug("toASMRule")

    val rule: StringBuilder = new StringBuilder()
    rule.append("rule " + ruleName + " = {").append(EOL)

    rule.append("  debuginfo " + ruleName + " \": adding process model '" + processModelID + "'\"").append(EOL).append(EOL)

    rule.append("  let processModelMap = ")

    appendASMCode(rule, processModelMap)

    rule.append("  in {").append(EOL)
    rule.append("    AddProcessModel(\"" + processModelID + "\", processModelMap)").append(EOL)
    rule.append("  }").append(EOL)

    rule.append("}")

    rule.toString()
  }


  private[asm] def appendASMCode(sb: StringBuilder, o: Any): StringBuilder = {
    logger.trace("appendASMCode_Any: " + o)

    o match {
      case x: String => appendASMCode(sb, x)
      case x: Int => appendASMCode(sb, x)
      case x: Long => appendASMCode(sb, x)
      case x: Double => appendASMCode(sb, x)
      case x: Boolean => appendASMCode(sb, x)
      case x: Seq[_] => appendASMCode(sb, x)
      case x: Set[_] => appendASMCode(sb, x)
      case x: Map[_, _] => appendASMCode(sb, x)

      case x => throw new IllegalArgumentException("unable to transform: " + x + "; class: " + x.getClass)
    }
  }

  private def appendASMCode(sb: StringBuilder, x: String): StringBuilder = {
    logger.trace("appendASMCode_String: " + x)

    sb.append("\"").append(x.replace(""""""", """\"""")).append("\"") // TODO: safe replace
  }

  private def appendASMCode(sb: StringBuilder, x: Int): StringBuilder = {
    logger.trace("appendASMCode_Int: " + x)

    sb.append(x)
  }

  private def appendASMCode(sb: StringBuilder, x: Long): StringBuilder = {
    logger.trace("appendASMCode_Long: " + x)

    sb.append(x)
  }

  private def appendASMCode(sb: StringBuilder, x: Double): StringBuilder = {
    logger.trace("appendASMCode_Double: " + x)

    sb.append(x)
  }

  private def appendASMCode(sb: StringBuilder, x: Boolean): StringBuilder = {
    logger.trace("appendASMCode_Boolean: " + x)

    sb.append(x)
  }

  private def appendASMCode(sb: StringBuilder, x: Seq[_]): StringBuilder = {
    logger.trace("appendASMCode_Seq: " + x)

    if (x == null || x.isEmpty) {
      sb.append("[]")
    }
    else {
      var first = true

      for (o <- x) {
        if (first) {
          first = false
          sb.append("[")
        }
        else {
          sb.append(", ")
        }

        appendASMCode(sb, o)
      }

      sb.append("]")

      sb
    }
  }

  private def appendASMCode(sb: StringBuilder, x: Set[_]): StringBuilder = {
    logger.trace("appendASMCode_Set: " + x)

    if (x == null || x.isEmpty) {
      sb.append("{}")
    }
    else {
      var first = true

      val maybeSorted = maybeSortedSet(x)

      for (o <- maybeSorted) {
        if (first) {
          first = false
          sb.append("{")
        }
        else {
          sb.append(", ")
        }

        appendASMCode(sb, o)
      }

      sb.append("}")

      sb
    }
  }

  private def appendASMCode(sb: StringBuilder , x: Map[_, _]): StringBuilder = {
    logger.trace("appendASMCode_Map: " + x)

    if (x == null || x.isEmpty) {
      sb.append("{->}")
    }
    else {
      var first = true

      val maybeSorted = maybeSortedMap(x)

      maybeSorted.foreach({ case (k, v) => {
        if (first) {
          first = false
          sb.append("{").append(EOL)
        }
        else {
          sb.append(",").append(EOL)
        }

        appendASMCode(sb, k)

        sb.append(" -> ")

        appendASMCode(sb, v)
      }})

      sb.append(EOL).append("}").append(EOL)

      sb
    }
  }

  private def maybeSortedSet(x: Set[_]): Set[_] = {
    if (x.forall(_.isInstanceOf[Int])) {
      TreeSet[Int]() ++ x.asInstanceOf[Set[Int]]
    }
    else if (x.forall(_.isInstanceOf[String])) {
      TreeSet[String]() ++ x.asInstanceOf[Set[String]]
    }
    else {
      x
    }
  }

  private def maybeSortedMap[X](x: Map[_, X]): Map[_, X] = {
    if (x.keys.forall(_.isInstanceOf[Int])) {
      TreeMap[Int, X]() ++ x.asInstanceOf[Map[Int, X]]
    }
    else if (x.keys.forall(_.isInstanceOf[String])) {
      TreeMap[String, X]() ++ x.asInstanceOf[Map[String, X]]
    }
    else {
      x
    }
  }
}
