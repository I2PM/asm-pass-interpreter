package de.athalis.pass.writer.asm

import org.slf4j.LoggerFactory

object ASMCodeGenerator {
  private val logger = LoggerFactory.getLogger(ASMCodeGenerator.getClass)

  def toASMRule(ruleName: String, processID: String, processMap: Map[String, _]): String = {
    logger.debug("toASMRule")

    val rule: StringBuilder = new StringBuilder()
    rule.append("rule " + ruleName + " = {\n")

    rule.append("  debuginfo " + ruleName + " \": adding process '" + processID + "'\"\n\n")

    rule.append("  let process = ")

    appendASMCode(rule, processMap)

    rule.append("  in {\n")
    rule.append("    AddProcess(\"" + processID + "\", process)\n")
    rule.append("  }\n")

    rule.append("}")

    rule.toString()
  }


  private def appendASMCode(sb: StringBuilder, o: Any): StringBuilder = {
    logger.trace("appendASMCode_Any: " + o)

    o match {
      case x: String => appendASMCode(sb, x)
      case x: Int => appendASMCode(sb, x)
      case x: Long => appendASMCode(sb, x)
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

      for (o <- x) {
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

      for ((k, v) <- x) {
        if (first) {
          first = false
          sb.append("{\n")
        }
        else {
          sb.append(",\n")
        }

        appendASMCode(sb, k)

        sb.append(" -> ")

        appendASMCode(sb, v)
      }

      sb.append("\n}\n")

      sb
    }
  }
}
