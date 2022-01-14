package de.athalis.pass.processmodel.parser.ast.node.pass

import de.athalis.pass.processmodel.parser.ast.node.CustomNode
import de.athalis.pass.processmodel.parser.ast.node.MapAbleNode

import org.slf4j.LoggerFactory

import java.util.NoSuchElementException

object SubjectNode {
  private val logger = LoggerFactory.getLogger(SubjectNode.getClass)

  val PARSER: org.jparsec.functors.Map3[MapAbleNode[String], Seq[(MapAbleNode[String], MapAbleNode[_])], Seq[MacroNode], SubjectNode] = (id, subjectParameters, macroNodes) => {
    logger.trace("SubjectNode create")

    //NodeDebugger.trace(from)

    val node = new SubjectNode(id.value)

    for (m <- macroNodes) node.addMacro(m)

    for (p <- subjectParameters) {
      node.setParameter(p._1.value, p._2.value)

      logger.trace("SubjectNode: added Node attribute: {}", p)
    }

    node
  }

  val PARSERInterfaceSubject: org.jparsec.functors.Map3[MapAbleNode[String], MapAbleNode[String], MapAbleNode[String], SubjectNode] = (id, externalSubjectID, externalProcessID) => {
    logger.trace("SubjectNode_Interface create")

    //NodeDebugger.trace(from)

    val node = new SubjectNode(id.value, true)
    node.externalSubjectID = externalSubjectID.value
    node.externalProcessID = externalProcessID.value

    node
  }

}

class SubjectNode(val id: String, val isInterfaceSubject: Boolean = false) extends CustomNode {
  var externalSubjectID: String = ""
  var externalProcessID: String = ""
  private var parameters: Map[String, Any] = Map()
  private var macros: Map[String, MacroNode] = Map()

  // add default parameters for normal subjects
  if (!isInterfaceSubject) {
    parameters += ("MainMacro" -> "Main")
    parameters += ("InputPool" -> 100)
    parameters += ("StartSubject" -> false)
  }


  def setParameter(k: String, v: Any): Unit = {
    this.parameters += (k -> v)
  }

  /*
  def getParameter(k: String): Any = this.parameters(k)
  */

  def addMacro(n: MacroNode): Unit = {
    n.setParent(this)
    macros += (n.id -> n)
  }

  def getMacros: Set[MacroNode] = this.macros.values.toSet

  def getMacroNumbers: Set[Int] = this.macros.values.map(_.macroNumber).toSet

  def getMainMacro: MacroNode = {
    val mainMacroID = this.parameters("MainMacro").asInstanceOf[String]

    val mn = this.macros.get(mainMacroID)

    if (mn.isEmpty) {
      throw new NoSuchElementException("Can not find Macro with the ID '" + mainMacroID + "'!")
    }

    mn.get
  }

  def getMainMacroNumber: Int = {
    this.getMainMacro.macroNumber
  }

  def getInputPoolSize: Int = this.parameters("InputPool").asInstanceOf[Int]

  def isStartSubject: Boolean = this.parameters.getOrElse("StartSubject", false).asInstanceOf[Boolean]

  override def toString: String = "SubjectNode '" + this.id + "'"
  def mkString: String = toString + "\n| Parameters: " + parameters.mkString(", ") + "\n| Macros: " + macros.mkString(", ")
}
