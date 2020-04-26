package de.athalis.pass.parser.ast.pass

import org.jparsec.functors.Map4
import org.slf4j.LoggerFactory
import de.athalis.pass.parser.ast._

object TransitionNode {
  object TransitionType extends Enumeration {
    type TransitionType = Value
    val Normal, Cancel, Timeout = Value
  }

  private val logger = LoggerFactory.getLogger(TransitionNode.getClass)

  private var nextTransitionNumber: Int = 0

  val PARSER: Map4[Option[MapAbleNode[String]], Option[CommunicationTransitionNode], Seq[TransitionProperty], MapAbleNode[String], TransitionNode] = (label, communicationTransitionProperties, transitionProperties, destination) => {
    logger.trace("TransitionNode create")

    //NodeDebugger.trace(from)

    val node = new TransitionNode(label.map(_.value))

    node.targetStateID = destination.value

    node.communicationProperties = communicationTransitionProperties

    transitionProperties foreach {
      case TransitionPropertyPriority(x) => { node.priority = x }
      case TransitionPropertyAuto => { node.auto = true }
      case TransitionPropertyHidden => { node.hidden = true }
      case TransitionPropertyTimeout(t) => { node.timeout = Some(t) }
      case TransitionPropertyCancel => { node.cancel = true }
    }

    node
  }

}

class TransitionNode(val label: Option[String]) extends CustomNode {
  import TransitionNode.TransitionType._

  val transitionNumber: Int = TransitionNode.nextTransitionNumber
  TransitionNode.nextTransitionNumber += 1

  var targetStateID: String = ""

  var communicationProperties: Option[CommunicationTransitionNode] = None

  var priority: Int = 0

  var auto: Boolean = false
  var hidden: Boolean = false
  var timeout: Option[Int] = None
  var cancel: Boolean = false

  def getType: TransitionType = {
    if (this.timeout.isDefined) {
      Timeout
    }
    else if (this.cancel) {
      Cancel
    }
    else {
      Normal
    }
  }


  def isAuto: Boolean = this.auto
  def isHidden: Boolean = this.hidden

  def getPriority: Int = this.priority
  def getTimeout: Int = this.timeout.getOrElse(0)

  override def toString: String = "TransitionNode '" + this.label + "'"
  def mkString: String = toString + "\n| Destination: " + targetStateID + "\n| priority: " + priority + "\n| auto: " + auto + "\n| hidden: " + hidden + "\n| timeout: " + timeout + "\n| cancel: " + cancel + "\n| communicationProperties: " + communicationProperties
}
