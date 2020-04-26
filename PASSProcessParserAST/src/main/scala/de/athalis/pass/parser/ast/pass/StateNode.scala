package de.athalis.pass.parser.ast.pass

import org.slf4j.LoggerFactory
import org.jparsec.functors.Map6

import de.athalis.pass.parser.ast._
import TransitionNode.TransitionType._

sealed trait StateProperty extends CustomNode
case class StatePropertyPriority(priority: Int) extends StateProperty

object StateNode {
  object StateType extends Enumeration {
    type StateType = Value
    val FunctionState, InternalAction, Send, Receive, End = Value
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
          case "End" => End
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

        if (node.stateType == End) {
          logger.error("End has outgoingTransitions!")
        }

        for (e <- transitions) {
          node.addOutgoingTransition(e)
          logger.trace("StateNode: added TransitionNode: {}", e)
        }
      }
      case None if (node.stateType == End) => {
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
  def referencesDefaultEndState: Boolean = this.outgoingTransitions.exists(_.targetStateID == "END")


  def addOutgoingTransition(e: TransitionNode): Unit = {
    if (this.stateType == Send && e.getType == Normal && this.outgoingTransitions.count(_.getType == Normal) == 1)
      throw new IllegalStateException("this send state has already a normal outgoing transition, cannot add " + e)

    e.setParent(this)
    outgoingTransitions += e
  }

  def getOutgoingTransitions: Set[TransitionNode] = this.outgoingTransitions
  def getNormalOutgoingTransitions: Set[TransitionNode] = this.outgoingTransitions.filter(_.getType == Normal)

  override def toString: String = "StateNode '" + this.id + "' (label: '" + this.label.getOrElse("") + "', type: " + this.stateType + ")"
  def mkStringParsedFrom: String = if (parsedFrom.isDefined) { "\n| parsedFrom: " + parsedFrom.get } else ""
  def mkString: String = toString + "\n| priority: " + priority + "\n| function: " + function + "\n| functionArguments: " + functionArguments + "\n| outgoingTransitions: " + outgoingTransitions + mkStringParsedFrom
}