package de.athalis.pass.semantic

import de.athalis.coreasm.base.Typedefs._

import de.athalis.pass.processmodel.tudarmstadt.Types.AgentIdentifier
import de.athalis.pass.processmodel.tudarmstadt.Types.MessageType
import de.athalis.pass.processmodel.tudarmstadt.Types.ProcessIdentifier
import de.athalis.pass.processmodel.tudarmstadt.Types.SubjectIdentifier

object Activities {
  import Helper._
  import Typedefs._

  sealed trait PASSActivity[T <: PASSActivityInput] {
    def toActivityString: String
    def getInput(inputGetter: InputGetter): T
    def getASMUpdates(input: T): Seq[ASMUpdate]

    def getASMUpdates(inputGetter: InputGetter): Seq[ASMUpdate] = {
      val input: T = getInput(inputGetter)
      val updates = getASMUpdates(input)
      updates
    }
  }

  sealed trait PASSActivityAgent[T <: PASSActivityInput] extends PASSActivity[T] { def state: ActiveState }
  sealed trait PASSActivityTask[T <: PASSActivityInput]  extends PASSActivity[T] { def task: TaskMap }

  sealed trait PASSActivityInput
  case object PASSActivityInputUnit extends PASSActivityInput
  case class PASSActivityInputAgents(names: Set[AgentIdentifier]) extends PASSActivityInput
  case class PASSActivityInputMessageContent(messageContent: String) extends PASSActivityInput
  case class PASSActivityInputSelection(selection: Option[Set[Int]]) extends PASSActivityInput

  trait InputGetter {
    def selectAgents(subjectID: SubjectIdentifier, min: Int, max: Int): PASSActivityInputAgents
    def setMessageContent(messageType: MessageType, receivers: Set[Channel]): PASSActivityInputMessageContent
    def performSelection(options: Seq[String], min: Int, max: Int): PASSActivityInputSelection
  }


  case class StartSubject(task: TaskMap, processModelID: ProcessIdentifier, processInstanceNumber: RuntimeProcessInstanceNumber, subjectID: SubjectIdentifier) extends PASSActivityTask[PASSActivityInputAgents] {

    override def toActivityString: String = {
      "Select Agent for Subject '" + subjectID + "' in Process Model '" + processModelID + "' (Instance " + processInstanceNumber + ") and start execution"
    }

    override def getInput(inputGetter: InputGetter): PASSActivityInputAgents = {
      inputGetter.selectAgents(subjectID, 1, 1)
    }

    override def getASMUpdates(input: PASSActivityInputAgents): Seq[ASMUpdate] = {
      if (input.names.size != 1)
        throw new IllegalArgumentException("There must be exactly one agent name")

      val startTask: TaskMap = initializeAndStartSubjectTask(processModelID, processInstanceNumber, subjectID, input.names.head)

      var updates: Seq[ASMUpdate] = Nil
      updates +:= addTask(startTask)
      updates +:= removeTask(task)
      updates
    }
  }

  case class TransitionDecision(transition: Transition) extends PASSActivityAgent[PASSActivityInputUnit.type] {
    def state: ActiveState = transition.sourceState

    override def toActivityString: String = {
      "Select Transition '" + transition.transitionLabel + "' to State '" + transition.targetStateLabel + "'"
    }

    override def getInput(inputGetter: InputGetter): PASSActivityInputUnit.type = PASSActivityInputUnit

    override def getASMUpdates(input: PASSActivityInputUnit.type): Seq[ASMUpdate] = {
      var updates: Seq[ASMUpdate] = Nil
      updates +:= SetValue(Seq("selectedTransition", state.ch.toSeq, state.MI, state.stateNumber), transition.transitionNumber)
      updates +:= SetValue(Seq("wantInput", state.ch.toSeq, state.MI, state.stateNumber), Set[String]())
      updates
    }
  }

  case class CommunicationTransitionDecision(transition: Transition, messages: Seq[String]) extends PASSActivityAgent[PASSActivityInputUnit.type] {
    def state: ActiveState = transition.sourceState

    override def toActivityString: String = {
      val prefix = if (messages.size > 1) {
        "Receive Messages: "
      } else {
        "Receive Message: "
      }

      prefix + messages.mkString("{", ", ", "}") + " | to State '" + transition.targetStateLabel + "'"
    }

    override def getInput(inputGetter: InputGetter): PASSActivityInputUnit.type = PASSActivityInputUnit

    override def getASMUpdates(input: PASSActivityInputUnit.type): Seq[ASMUpdate] = {
      var updates: Seq[ASMUpdate] = Nil
      updates +:= SetValue(Seq("selectedTransition", state.ch.toSeq, state.MI, state.stateNumber), transition.transitionNumber)
      updates +:= SetValue(Seq("wantInput", state.ch.toSeq, state.MI, state.stateNumber), Set[String]())
      updates
    }
  }

  case class CancelDecision(transition: Transition) extends PASSActivityAgent[PASSActivityInputUnit.type] {
    def state: ActiveState = transition.sourceState

    override def toActivityString: String = {
      val transitionLabel = transition.transitionLabel match {
        case "undefined" => ""
        case x => " '"+x+"'"
      }
      "Select Cancel" + transitionLabel + " to State '" + transition.targetStateLabel + "'"
    }

    override def getInput(inputGetter: InputGetter): PASSActivityInputUnit.type = PASSActivityInputUnit

    override def getASMUpdates(input: PASSActivityInputUnit.type): Seq[ASMUpdate] = {
      var updates: Seq[ASMUpdate] = Nil
      updates +:= SetValue(Seq("cancelDecision", state.ch.toSeq, state.MI, state.stateNumber), true)
      updates +:= SetValue(Seq("wantInput", state.ch.toSeq, state.MI, state.stateNumber), Set[String]())
      updates
    }
  }

  case class MessageContentDecision(state: ActiveState, messageType: MessageType, receivers: Set[Channel]) extends PASSActivityAgent[PASSActivityInputMessageContent] {
    override def toActivityString: String = {
      "Set Message Content (\"" + messageType + "\") for Message to " + receivers.mkString("{", ", ", "}")
    }

    override def getInput(inputGetter: InputGetter): PASSActivityInputMessageContent = {
      inputGetter.setMessageContent(messageType, receivers)
    }

    override def getASMUpdates(input: PASSActivityInputMessageContent): Seq[ASMUpdate] = {
      var updates: Seq[ASMUpdate] = Nil
      updates +:= SetValue(Seq("messageContent", state.ch.toSeq, state.MI, state.stateNumber), Seq("TextContent", input.messageContent))
      updates +:= SetValue(Seq("wantInput", state.ch.toSeq, state.MI, state.stateNumber), Set[String]())
      updates
    }
  }

  case class SelectionDecision(state: ActiveState, options: Seq[String], min: Int, max: Int) extends PASSActivityAgent[PASSActivityInputSelection] {
    override def toActivityString: String = {
      "Perform Selection"
    }

    override def getInput(inputGetter: InputGetter): PASSActivityInputSelection = {
      inputGetter.performSelection(options, min, max)
    }

    override def getASMUpdates(input: PASSActivityInputSelection): Seq[ASMUpdate] = {
      if (input.selection.isEmpty)
        throw new IllegalArgumentException("Selection had been aborted, unable to generate updates for that")

      var updates: Seq[ASMUpdate] = Nil
      updates +:= SetValue(Seq("selectionDecision", state.ch.toSeq, state.MI, state.stateNumber), input.selection.get)
      updates +:= SetValue(Seq("wantInput", state.ch.toSeq, state.MI, state.stateNumber), Set[String]())
      updates
    }
  }

  case class SelectAgents(state: ActiveState, processModelID: ProcessIdentifier, subjectID: SubjectIdentifier, min: Int, max: Int) extends PASSActivityAgent[PASSActivityInputAgents] {
    override def toActivityString: String = {
      val text = {
        if (max == 1) "Select agent"
        else if (min == max) "Select " + min + " Agents"
        else if (max == 0) "Select at least " + min + " Agents"
        else "Select " + min + " to " + max + " Agents"
      }

      val suffix = if (processModelID != state.ch.processModelID) {
        " in Process Model '" + processModelID + "'"
      } else ""

      text + " for Subject '" + subjectID + "'" + suffix
    }

    override def getInput(inputGetter: InputGetter): PASSActivityInputAgents = {
      inputGetter.selectAgents(subjectID, min, max)
    }

    override def getASMUpdates(input: PASSActivityInputAgents): Seq[ASMUpdate] = {
      var updates: Seq[ASMUpdate] = Nil
      updates +:= SetValue(Seq("selectAgentsDecision", state.ch.toSeq, state.MI, state.stateNumber), input.names)
      updates +:= SetValue(Seq("wantInput", state.ch.toSeq, state.MI, state.stateNumber), Set[String]())
      updates
    }
  }

}
