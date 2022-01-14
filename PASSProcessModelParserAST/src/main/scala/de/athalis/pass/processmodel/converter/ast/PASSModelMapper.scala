package de.athalis.pass.processmodel.converter.ast

import de.athalis.pass.processmodel.operation.PASSProcessModelConverterSingle
import de.athalis.pass.processmodel.parser.ast.node.pass._
import de.athalis.pass.processmodel.tudarmstadt.Types._
import de.athalis.pass.processmodel.tudarmstadt._

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

object PASSModelMapper extends PASSProcessModelConverterSingle[ProcessNode, Process] {

  private def toNodeType(stn: StateNode): Option[Function] = stn.stateType match {
    case StateNode.StateType.InternalAction => {
      None
    }
    case StateNode.StateType.Terminate => {
      val result: Option[String] = stn.functionArguments.flatMap(_.headOption.collect { case x: String => x })
      Some(Terminate(result))
    }
    case StateNode.StateType.Return => {
      val result: Option[String] = stn.functionArguments.flatMap(_.headOption.collect { case x: String => x })
      Some(Return(result))
    }
    case StateNode.StateType.Send => {
      val autoTransitions = stn.getNormalOutgoingTransitions.filter(_.isAuto)
      val manualTransitions = stn.getNormalOutgoingTransitions.filterNot(_.isAuto)

      val isAuto = manualTransitions.isEmpty
      val isManual = autoTransitions.isEmpty

      if (isAuto) {
        Some(AutoSend)
      }
      else if (isManual) {
        Some(ManualSend)
      }
      else {
        throw new Exception("send is neither pure auto nor pure manual: " + stn)
      }
    }
    case StateNode.StateType.Receive => {
      val autoTransitions = stn.getNormalOutgoingTransitions.filter(_.isAuto)
      val manualTransitions = stn.getNormalOutgoingTransitions.filterNot(_.isAuto)

      val isAuto = manualTransitions.isEmpty
      val isManual = autoTransitions.isEmpty

      if (isAuto) {
        Some(AutoReceive)
      }
      else if (isManual) {
        Some(ManualReceive)
      }
      else {
        throw new Exception("receive is neither pure auto nor pure manual: " + stn)
      }
    }
    case StateNode.StateType.FunctionState => {
      val args = stn.functionArguments

      stn.function match {
        case "Tau" => Some(Tau)
        case "CallMacro" => {
          val a = args.get.asInstanceOf[Seq[String]]
          Some(CallMacro(a.head, a.tail))
        }
        case "Cancel" => Some(Cancel)
        case "ModalSplit" => Some(ModalSplit)
        case "ModalJoin" => Some(ModalJoin)
        case "VarMan" => {
          val a = args.get.asInstanceOf[Seq[String]]
          Some(VarMan(a.head, a.tail))
        }
        case "CloseIP" => {
          val a = args.get
          val cID = a(2) match {
            case 0 | "" => None
            case x: String => Some(x)
          }
          Some(CloseIP(a(0).asInstanceOf[String], a(1).asInstanceOf[String], cID))
        }
        case "OpenIP" => {
          val a = args.get
          val cID = a(2) match {
            case 0 | "" => None
            case x: String => Some(x)
          }
          Some(OpenIP(a(0).asInstanceOf[String], a(1).asInstanceOf[String], cID))
        }
        case "IsIPEmpty" => {
          val a = args.get
          val cID = a(2) match {
            case 0 | "" => None
            case x: String => Some(x)
          }
          Some(IsIPEmpty(a(0).asInstanceOf[String], a(1).asInstanceOf[String], cID))
        }
        case "CloseAllIPs" => Some(CloseAllIPs)
        case "OpenAllIPs" => Some(OpenAllIPs)
        case "SelectAgents" => {
          val a = args.get
          Some(SelectAgents(a(0).asInstanceOf[String], a(1).asInstanceOf[String], a(2).asInstanceOf[Int], a(3).asInstanceOf[Int]))
        }
        case x => throw new IllegalArgumentException("unknown function: " + x)
      }
    }
  }

  private def toPASSTransition(t: TransitionNode, function: Option[Function]): Transition = {
    val label: Option[String] = t.label
    val targetIdentifier: StateIdentifier = t.targetStateID // why not internalIdentifier? because then we would need to find a reference to it..
    var attributes = Set.empty[TransitionAttribute]

    if (t.isAuto) {
      function match {
        case Some(s) if s.isInstanceOf[ManualFunction] => throw new Exception("Manual function with auto transition: " + t)
        case _ => ()
      }
    }

    if (t.isHidden) {
      attributes += TransitionIsHidden
    }

    if (t.priority != 0) {
      attributes += TransitionHasPriority(t.priority)
    }

    t.getType match {
      case TransitionNode.TransitionType.Timeout => {
        attributes += TransitionHasTimeout(FiniteDuration(t.getTimeout.toLong, TimeUnit.SECONDS))
      }
      case TransitionNode.TransitionType.Cancel => {
        attributes += TransitionIsCancel
      }
      case TransitionNode.TransitionType.Normal => {
        ()
      }
    }

    var hasActionExitParameter = label.isDefined // might be false
    var exitParameter: Option[TransitionCondition] = None

    t.communicationProperties match {
      case Some(props) => {
        implicit class RichString(self: String) {
          def asOption: Option[String] = {
            if (self == "") {
              None
            }
            else {
              Some(self)
            }
          }
        }


        // TODO: clean this up in ast.pass
        val messageType: MessageType = props.msgType
        val subject: SubjectIdentifier = props.subject
        val subjectVariable: Option[VariableIdentifier] = props.subjectVar.asOption
        val subjectCount: SubjectCount = (props.subjectCountMin, props.subjectCountMax)
        val contentVariable: Option[VariableIdentifier] = props.content_var.asOption
        val senderCorrelationVariable: Option[VariableIdentifier] = props.new_correlation_var.asOption
        val receiverCorrelationVariable: Option[VariableIdentifier] = props.with_correlation_var.asOption
        val storeReceiverVariable: Option[VariableIdentifier] = props.store_receiver_var.asOption
        val storeMessagesVariable: Option[VariableIdentifier] = props.store_messages_var.asOption

        if (storeReceiverVariable.isDefined && storeMessagesVariable.isDefined) {
          throw new Exception("both storeReceiverVariable and storeMessagesVariable defined: " + t)
        }

        val storeVariable: Option[VariableIdentifier] = storeReceiverVariable.orElse(storeMessagesVariable)

        val interactionExitParameter = MessageExchangeCondition(
          messageType,
          subject,
          subjectVariable,
          subjectCount,
          contentVariable,
          senderCorrelationVariable,
          receiverCorrelationVariable,
          storeVariable,
          label
        )

        if (interactionExitParameter.copy(label = None) != MessageExchangeCondition("", "", None, (1, 1), None, None, None, None, None)) {
          exitParameter = Some(interactionExitParameter)
          hasActionExitParameter = false
        }
      }
      case None => {
        ()
      }
    }

    if (hasActionExitParameter) {
      exitParameter = Some(DoTransitionCondition(label.get))
    }

    Transition(targetIdentifier, exitParameter, attributes)
  }

  private def toPASSAction(stn: StateNode): Action = {
    val identifier: StateIdentifier = stn.id
    val label: Option[String] = stn.label
    val function: Option[Function] = toNodeType(stn)
    val transitions: Set[Transition] = stn.getOutgoingTransitions.map(t => toPASSTransition(t, function))
    var attributes: Set[StateAttribute] = Set.empty

    // TODO: Additional Semantics

    if (stn.priority != 0) {
      attributes += StateHasPriority(stn.priority)
    }

    val node = State(label, attributes, function)
    Action(identifier, node, transitions)
  }

  private def toPASSMacro(mn: MacroNode): Macro = {
    val identifier: MacroIdentifier = mn.id
    val actions: Set[Action] = mn.getStates.map(toPASSAction)
    val startAction: StateIdentifier = mn.getStartState.get.id
    val arguments: Seq[MacroArgument] = mn.getMacroArguments
    val localVariables: Set[VariableIdentifier] = mn.getMacroVariables

    Macro(identifier, startAction, actions, arguments, localVariables)
  }

  private def toPASSSubject(sn: SubjectNode): Subject = {
    if (sn.isInterfaceSubject) {
      val maximumInstanceRestriction: Option[Int] = None // FIXME

      if (sn.externalProcessID == "?" && sn.externalSubjectID == "?") {
        UndefinedInterfaceSubject(sn.id, maximumInstanceRestriction)
      }
      else if (sn.externalProcessID == "?" || sn.externalSubjectID == "?") {
        throw new Exception("invalid external subject: " + sn)
      }
      else if (sn.externalProcessID == "" || sn.externalSubjectID == "") {
        throw new Exception("invalid external subject: " + sn)
      }
      else {
        DefinedInterfaceSubject(sn.id, sn.externalProcessID, sn.externalSubjectID, maximumInstanceRestriction)
      }
    }
    else {
      val identifier: SubjectIdentifier = sn.id
      val inputPoolSize: Int = sn.getInputPoolSize
      val macros: Set[Macro] = sn.getMacros.map(toPASSMacro)
      val mainMacroID: MacroIdentifier = sn.getMainMacro.id

      val mainMacro: Macro = macros.find(_.identifier == mainMacroID).get
      val additionalMacros: Set[Macro] = (macros - mainMacro)

      val internalBehavior = InternalBehavior(mainMacro, additionalMacros)

      val isStartSubject = sn.isStartSubject
      val maximumInstanceRestriction: Option[Int] = None // FIXME

      var attributes: Set[SubjectAttribute] = Set.empty

      attributes += SubjectHasInputPoolSize(inputPoolSize)

      if (isStartSubject) {
        attributes += SubjectIsStartSubject
      }

      // TODO: Additional Semantics

      FullySpecifiedSubject(identifier, internalBehavior, maximumInstanceRestriction, attributes)
    }
  }

  override def convertSingle(p: ProcessNode): Process = {
    val identifier: ProcessIdentifier = p.id
    val subjects: Set[Subject] = p.getSubjects.map(toPASSSubject)
    val macros: Set[Macro] = p.getMacros.map(toPASSMacro)

    // TODO
    val subjectToSubjectCommunication: Option[Set[MessageExchanges]] = None

    val attributes: Set[ProcessAttribute] = p.getData.collect {
      case ("agents", x) => {
        val y = x.asInstanceOf[Map[String, Set[String]]]
        ProcessAgents(y)
      }
      case (k, v) => ProcessData(k, v)
    }.toSet

    Process(identifier, subjects, subjectToSubjectCommunication, macros, attributes)
  }

}
