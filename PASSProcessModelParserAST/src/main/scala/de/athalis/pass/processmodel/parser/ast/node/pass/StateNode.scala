package de.athalis.pass.processmodel.parser.ast.node.pass

import de.athalis.pass.processmodel.parser.ast.node._

import org.jparsec.functors.Map6

import org.slf4j.LoggerFactory

import java.lang.System.{lineSeparator => EOL}

import TransitionNode.TransitionType._

sealed trait StateProperty extends CustomNode
case class StatePropertyPriority(priority: Int) extends StateProperty

object StateNode {
  object StateType extends Enumeration {
    type StateType = Value
    val FunctionState, InternalAction, Send, Receive, Terminate, Return = Value
  }
  import StateType._

  private val logger = LoggerFactory.getLogger(StateNode.getClass)

  private var nextStateNumber: Int = 0

  val PARSER: Map6[MapAbleNode[String], Option[MapAbleNode[String]], MapAbleNode[String], Option[Seq[StateProperty]], Option[Seq[MapAbleNode[_]]], Option[Seq[TransitionNode]], StateNode] = (id, label, stateType, stateProperties, functionArguments, outgoingTransitions) => {
    logger.trace("StateNode create")

    //NodeDebugger.trace(from)

    val node = new StateNode(id.value)

    node.label = label.map(_.value)

    stateType match {
      case idNode: IDNode => {
        logger.trace("StateNode: IDNode => predefined state")
        node.stateType = idNode.value match {
          case "InternalAction" => InternalAction
          case "Send" => Send
          case "Receive" => Receive
          case "Terminate" => Terminate
          case "Return" => Return
          case x => throw new IllegalArgumentException("unknown predefined state type: '" + x + "'")
        }
      }
      case stringNode: STRINGNode => {
        logger.trace("StateNode: STRINGNode => function state")
        node.stateType = FunctionState
        node.function = stringNode.value
      }
    }

    stateProperties foreach (_ foreach {
      case StatePropertyPriority(p) => { node.priority = p }
    })

    node.functionArguments = functionArguments.map(_.map(_.value))

    // Transitions

    // multiple outgoing transitions
    outgoingTransitions match {
      case Some(transitions) => {
        logger.trace("StateNode outgoingTransitions: {}", transitions)

        if (node.stateType == Terminate) {
          logger.error("Terminate has outgoingTransitions!")
        }
        else if (node.stateType == Return) {
          logger.error("Return has outgoingTransitions!")
        }

        for (e <- transitions) {
          node.addOutgoingTransition(e)
          logger.trace("StateNode: added TransitionNode: {}", e)
        }
      }
      case None if (node.stateType == Terminate) => {
        // this is ok, nothing to do
      }
      case None if (node.stateType == Return) => {
        // this is ok, nothing to do
      }
      case None => {
        throw new IllegalArgumentException("State '" + node.id + "' (label: " + node.label + ") " + " has no outgoing transitions! (State type: " + node.stateType + ")")
      }
    }

    node
  }

}

class StateNode(val id: String, private val parsedFrom: Option[Any] = None) extends CustomNode {
  import StateNode.StateType._

  val stateNumber: Int = StateNode.nextStateNumber
  StateNode.nextStateNumber += 1

  var stateType: StateType = InternalAction

  var label: Option[String] = None

  var function: String = ""
  var functionArguments: Option[Seq[Any]] = None
  var priority: Int = 0
  private var outgoingTransitions: Set[TransitionNode] = Set()

  def isCommunicationState: Boolean = ((this.stateType == Send) || (this.stateType == Receive))
  def referencesDefaultTerminateState: Boolean = this.outgoingTransitions.exists(_.targetStateID == "TERMINATE")
  def referencesDefaultReturnState: Boolean = this.outgoingTransitions.exists(_.targetStateID == "RETURN")


  def addOutgoingTransition(e: TransitionNode): Unit = {
    if (this.stateType == Send && e.getType == Normal && this.outgoingTransitions.count(_.getType == Normal) == 1)
      throw new IllegalStateException("this send state has already a normal outgoing transition, cannot add " + e)

    e.setParent(this)
    outgoingTransitions += e
  }

  def getOutgoingTransitions: Set[TransitionNode] = this.outgoingTransitions
  def getNormalOutgoingTransitions: Set[TransitionNode] = this.outgoingTransitions.filter(_.getType == Normal)

  override def toString: String = "StateNode '" + this.id + "' (label: '" + this.label.getOrElse("") + "', type: " + this.stateType + ")"
  def mkStringParsedFrom: String = if (parsedFrom.isDefined) { EOL + "| parsedFrom: " + parsedFrom.get } else ""
  def mkString: String = toString + EOL + "| priority: " + priority + EOL + "| function: " + function + EOL + "| functionArguments: " + functionArguments + EOL + "| outgoingTransitions: " + outgoingTransitions + mkStringParsedFrom
}
