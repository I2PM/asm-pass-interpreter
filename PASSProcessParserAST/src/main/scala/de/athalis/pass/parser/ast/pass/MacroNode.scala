package de.athalis.pass.parser.ast.pass

import java.util.NoSuchElementException

import org.slf4j.LoggerFactory

import de.athalis.pass.parser.ast._
import StateNode.StateType._

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

      for ((macroParameterKey, macroParamterValue) <- macroParameters) {
        logger.trace("MacroNode: adding Node attribute: " + macroParameterKey + " -> " + macroParamterValue)

        node.setParameter(macroParameterKey.value, macroParamterValue.value)
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

  private var hasAddedDefaultEndState: Boolean = false

  // add default parameters
  parameters += ("StartState" -> "START")


  def addDefaultEndState(): Unit = if (!hasAddedDefaultEndState) {
    val end = new StateNode("TERMINATE")
    end.setParent(this)
    end.stateType = Terminate

    this.addState(end)
    hasAddedDefaultEndState = true
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

    if (state.referencesDefaultEndState) {
      this.addDefaultEndState()
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
  def mkString: String = toString + "\n| Arguments: " + arguments.mkString(", ") + "\n| Parameters: " + parameters.mkString(", ") + "\n| States: " + states.mkString(", ")
}
