package de.athalis.pass.processmodel.parser.ast.node.pass

import de.athalis.pass.processmodel.parser.ast.node.CustomNode
import de.athalis.pass.processmodel.parser.ast.node.MapAbleNode
import de.athalis.pass.processmodel.parser.ast.node.pass.StateNode.StateType._

import org.slf4j.LoggerFactory

import java.lang.System.{lineSeparator => EOL}
import java.util.NoSuchElementException

object MacroNode {
  val PARSER = new NodeParser(false)
  val PARSERProcessMacro = new NodeParser(true)

  private val logger = LoggerFactory.getLogger(MacroNode.getClass)

  private var nextMacroNumber: Int = 0


  class NodeParser(val isProcessMacro: Boolean) extends org.jparsec.functors.Map4[MapAbleNode[String], Option[Seq[MapAbleNode[String]]], Seq[(MapAbleNode[String], MapAbleNode[_])], Seq[StateNode], MacroNode] {

    override def map(id: MapAbleNode[String], macroProperties: Option[Seq[MapAbleNode[String]]], macroParameters: Seq[(MapAbleNode[String], MapAbleNode[_])], stateNodes: Seq[StateNode]): MacroNode = {
      logger.trace("MacroNode create")

      //NodeDebugger.trace(from)

      val node = new MacroNode(id.value, isProcessMacro)

      node.arguments = macroProperties.map(_.map(_.value))


      for (stateNode <- stateNodes) {
        node.addState(stateNode)
        logger.trace("MacroNode: added StateNode: " + stateNode)
      }

      for ((macroParameterKey, macroParameterValue) <- macroParameters) {
        logger.trace("MacroNode: adding Node attribute: " + macroParameterKey + " -> " + macroParameterValue)

        node.setParameter(macroParameterKey.value, macroParameterValue.value)
      }

      node
    }
  }


}

class MacroNode(val id: String, val isProcessMacro: Boolean) extends CustomNode {

  val macroNumber: Int = MacroNode.nextMacroNumber
  MacroNode.nextMacroNumber += 1

  var arguments: Option[Seq[String]] = None
  private var parameters: Map[String, Any] = Map()
  private var states: Map[String, StateNode] = Map()

  private var hasAddedDefaultTerminateState: Boolean = false
  private var hasAddedDefaultReturnState: Boolean = false

  // add default parameters
  parameters += ("StartState" -> "START")


  def addDefaultTerminateState(): Unit = if (!hasAddedDefaultTerminateState) {
    val end = new StateNode("TERMINATE")
    end.setParent(this)
    end.stateType = Terminate

    this.addState(end)
    hasAddedDefaultTerminateState = true
  }

  def addDefaultReturnState(): Unit = if (!hasAddedDefaultReturnState) {
    val returnState = new StateNode("RETURN")
    returnState.setParent(this)
    returnState.stateType = Return

    this.addState(returnState)
    hasAddedDefaultReturnState = true
  }


  def setParameter(k: String, v: Any): Unit = {
    this.parameters += (k -> v)
  }


  def getMacroArguments: Seq[String] = this.arguments.getOrElse(Seq())

  def getMacroVariables: Set[String] = {
    val vars = this.parameters.get("LocalVariables")

    if (vars.isEmpty) {
      Set()
    }
    else{
      vars.get.asInstanceOf[Set[String]]
    }
  }

  def addState(state: StateNode): Unit = {
    state.setParent(this)
    this.states += (state.id -> state)

    if (state.referencesDefaultTerminateState) {
      this.addDefaultTerminateState()
    }
    else if (state.referencesDefaultReturnState) {
      this.addDefaultReturnState()
    }
  }

  def getStates: Set[StateNode] = this.states.values.toSet

  def getState(stateID: String): Option[StateNode] = this.states.get(stateID)

  def getStateNumber(stateID: String): Int = {
    val sn = this.states.get(stateID)

    if (sn.isEmpty) {
      throw new NoSuchElementException("Can not find State with the id '" + stateID + "'")
    }
    else {
      sn.get.stateNumber
    }
  }

  def getStartStateNumber: Int = {
    val stateID = this.parameters("StartState").asInstanceOf[String]
    this.getStateNumber(stateID)
  }

  def getStartState: Option[StateNode] = {
    val stateID = this.parameters("StartState").asInstanceOf[String]
    this.getState(stateID)
  }

  def getStateNumbers: Set[Int] = this.states.values.map(_.stateNumber).toSet

  override def toString: String = "MacroNode '" + this.id + "'"
  def mkString: String = toString + EOL + "| Arguments: " + arguments.mkString(", ") + EOL + "| Parameters: " + parameters.mkString(", ") + EOL + "| States: " + states.mkString(", ")
}
