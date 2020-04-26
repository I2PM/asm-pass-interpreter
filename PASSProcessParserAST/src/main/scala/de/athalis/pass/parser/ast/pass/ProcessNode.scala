package de.athalis.pass.parser.ast.pass

import org.slf4j.LoggerFactory
import de.athalis.pass.parser.ast._

object ProcessNode {
  private val logger = LoggerFactory.getLogger(ProcessNode.getClass)

  val PARSER: java.util.function.BiFunction[MapAbleNode[String], Seq[CustomNode], ProcessNode] = (id, children) => {
    logger.trace("ProcessNode create")

    //NodeDebugger.trace(from)

    val node: ProcessNode = new ProcessNode(id.value)

    for (n: CustomNode <- children) {
      n match {
        case x: DataNode => node.addData(x)
        case x: SubjectNode => node.addSubject(x)
        case x: MacroNode => node.addMacro(x)
        case _ => throw new IllegalArgumentException("ProcessNode: unknown children: " + n)
      }
    }

    logger.trace("ProcessNode created!")

    node
  }
}

class ProcessNode(val id: String) extends CustomNode {
  private var subjects = Map.empty[String, SubjectNode]
  private var macros = Map.empty[String, MacroNode]
  private var data = Map.empty[String, Any]

  def addSubject(n: SubjectNode): Unit = {
    n.setParent(this)
    this.subjects += (n.id -> n)
  }

  def getSubjects: Set[SubjectNode] = subjects.values.toSet

  def addMacro(n: MacroNode): Unit = {
    n.setParent(this)
    this.macros += (n.id -> n)
  }

  def getMacros: Set[MacroNode] = this.macros.values.toSet

  def getAllMacros: Set[MacroNode] = (this.getMacros ++ this.getSubjects.flatMap(_.getMacros))

  def getData: Map[String, Any] = data

  def addData(data: DataNode): Unit = {
    this.data ++= data.value
  }

  override def toString: String = "ProcessNode '" + this.id + "'"
  def mkString(): String = toString + "\n| Data: " + data.mkString(", ") + "\n| Macros: " + macros.mkString(", ") + "\n| Subjects: " + subjects.mkString(", ")
}
