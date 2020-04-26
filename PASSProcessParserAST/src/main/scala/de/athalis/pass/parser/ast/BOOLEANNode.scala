package de.athalis.pass.parser.ast

object BOOLEANNode {
  val PARSER: java.util.function.Function[MapAbleNode[String], MapAbleNode[Boolean]] = (from) => from.value match {
    case "true" => BOOLEANNode(true)
    case "false" => BOOLEANNode(false)
    case x => throw new IllegalArgumentException("""expected either "true" or "false", but it is """ + x + """"""")
  }
}

case class BOOLEANNode(value: Boolean) extends CustomNode with MapAbleNode[Boolean]
