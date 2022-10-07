package de.athalis.pass.processmodel.writer.asm

import de.athalis.pass.processmodel.PASSProcessModel
import de.athalis.pass.processmodel.PASSProcessModelCollection
import de.athalis.pass.processmodel.operation.PASSProcessModelConverterSingle
import de.athalis.pass.processmodel.tudarmstadt.Types._
import de.athalis.pass.processmodel.tudarmstadt._
import de.athalis.pass.processmodel.writer.asm.TUDarmstadtModel2ASMMap.ProcessModelASMMap
import de.athalis.pass.processmodel.writer.asm.TUDarmstadtModel2ASMMap.ProcessModelsASMMap

case class ProcessModelASMMapWrapper(processModelID: ProcessIdentifier, map: ProcessModelASMMap) extends PASSProcessModel

object ProcessModelASMMapWrapper {
  def toProcessModelsASMMap(processModels: PASSProcessModelCollection[ProcessModelASMMapWrapper]): ProcessModelsASMMap = {
    processModels.getProcessModels[ProcessModelASMMapWrapper].map(p => {
        (p.processModelID -> p.map)
      }).toMap
  }
}

object TUDarmstadtModel2ASMMap extends PASSProcessModelConverterSingle[Process, ProcessModelASMMapWrapper] {
  type ProcessModelASMMap = Map[String, Any]
  type ProcessModelsASMMap = Map[ProcessIdentifier, ProcessModelASMMap]

  private[asm] def getFunctionName(x: Function): Option[FunctionName] = x match {
    case Tau => Some("Tau")
    case CallMacro(_, _) => Some("CallMacro")
    case Cancel => Some("Cancel")
    case ModalSplit => Some("ModalSplit")
    case ModalJoinRich(_) => Some("ModalJoin")
    case VarMan(_, _) => Some("VarMan")
    case CloseIP(_, _, _) => Some("CloseIP")
    case OpenIP(_, _, _) => Some("OpenIP")
    case IsIPEmpty(_, _, _) => Some("IsIPEmpty")
    case CloseAllIPs => Some("CloseAllIPs")
    case OpenAllIPs => Some("OpenAllIPs")
    case SelectAgents(_, _, _, _) => Some("SelectAgents")
    case _ => None
  }

  private def getFunctionArguments(x: Function): Seq[FunctionArgument] = x match {
    case CallMacro(macroID, macroArguments) => macroID +: macroArguments
    case VarMan(method, methodArguments) => method.toString +: methodArguments
    case CloseIP(senderSubjID, messageType, correlationID) => {
      val cID = correlationID match {
        case None => 0
        case Some(x) => x
      }
      Seq(senderSubjID, messageType, cID)
    }
    case OpenIP(senderSubjID, messageType, correlationID) => {
      val cID = correlationID match {
        case None => 0
        case Some(x) => x
      }
      Seq(senderSubjID, messageType, cID)
    }
    case IsIPEmpty(senderSubjID, messageType, correlationID) => {
      val cID = correlationID match {
        case None => 0
        case Some(x) => x
      }
      Seq(senderSubjID, messageType, cID)
    }
    case SelectAgents(destination, subject, countMin, countMax) => Seq(destination, subject, countMin, countMax)
    case ModalJoinRich(joinCount) => Seq(joinCount)
    case Tau | Cancel | ModalSplit | CloseAllIPs | OpenAllIPs | _ => Seq.empty
  }

  private def getStateType(typ: Option[Function]): String = typ match {
    // Note: InternalAction does not exist in OWL, only DoState.
    // Both FunctionState and InternalAction are just DoState.
    case None => "internalAction"
    case Some(Terminate(_) | Return(_)) => "terminate"
    case Some(AutoSend) | Some(ManualSend) => "send"
    case Some(AutoReceive) | Some(ManualReceive) => "receive"
    case _ => "function"
  }

  override def convertSingle(processModel: Process): ProcessModelASMMapWrapper = {
    ProcessModelASMMapWrapper(processModel.identifier, toMap(processModel))
  }

  def toMap(processModel: Process): ProcessModelASMMap = {
    // types used by the Semantic
    type InternalNumberMacro = Int
    type InternalNumberState = Int
    type InternalNumberTransition = Int

    object InternalNumberWrapped
    {
      private var lastNumber = 0
      def construct[A, B](x: A, parent: B): InternalNumberWrapped[A, B] = {
        lastNumber = lastNumber + 1
        InternalNumberWrapped[A, B](lastNumber, x, parent)
      }
    }
    case class InternalNumberWrapped[A, B](internalNumber: Int, value: A, parent: B)

    type MacroWithInternalNumber = InternalNumberWrapped[Macro, Option[Subject]]
    type ActionWithInternalNumber = InternalNumberWrapped[Action, MacroWithInternalNumber]
    type TransitionWithInternalNumber = InternalNumberWrapped[Transition, ActionWithInternalNumber]

    val processAttributes: Set[ProcessAttribute] = processModel.attributes

    val subjects: Set[Subject] = processModel.subjects
    val startSubjectIDs: Set[SubjectIdentifier] = subjects.collect({
      case s: FullySpecifiedSubject if s.attributes.contains(SubjectIsStartSubject) => s.identifier
    })

    val processMacros: Set[MacroWithInternalNumber] = processModel.macros.map(m => InternalNumberWrapped.construct[Macro, Option[Subject]](m, None))
    val macros: Set[MacroWithInternalNumber] = processMacros ++ subjects.collect{case s: FullySpecifiedSubject => (s.internalBehavior.additionalMacros + s.internalBehavior.mainMacro).map(m => InternalNumberWrapped.construct[Macro, Option[Subject]](m, Some(s)))}.flatten
    val processMacroNumbers: Set[InternalNumberMacro] = processMacros.map(_.internalNumber)

    val actions: Set[ActionWithInternalNumber] = macros.flatMap(m => m.value.actions.map(n => InternalNumberWrapped.construct[Action, MacroWithInternalNumber](n, m)))

    val transitions: Set[TransitionWithInternalNumber] = actions.flatMap(n => n.value.outgoingTransitions.map(e => InternalNumberWrapped.construct[Transition, ActionWithInternalNumber](e, n)))

    val subjectsMap: Map[String, Map[String, _]] = subjects.map {
      case s: UndefinedInterfaceSubject => {
        val x: Map[String, Any] = Map(
          "interface" -> Seq("?", "?")
        )
        (s.identifier -> x)
      }
      case s: DefinedInterfaceSubject => {
        val x: Map[String, Any] = Map(
          "interface" -> Seq(s.process, s.subject)
        )
        (s.identifier -> x)
      }
      case s: FullySpecifiedSubject => {
        val myMacros = macros.filter(_.parent == Some(s))

        val ipSize: Int = s.attributes.collectFirst {
          case SubjectHasInputPoolSize(x) => x
        }.getOrElse(-1)

        val x: Map[String, Any] = Map(
          ("interface" -> false),
          ("macroNumbers" -> myMacros.map(_.internalNumber)),
          ("mainMacroNumber" -> myMacros.find(_.value == s.internalBehavior.mainMacro).get.internalNumber),
          ("inputpoolSize" -> ipSize)
        )
        (s.identifier -> x)
      }
    }.toMap


    val macrosMap: Map[Int, Map[String, Any]] = macros.map(m_ => {
      val m: Macro = m_.value
      val macroActions = actions.filter(_.parent == m_)

      val x: Map[String, Any] = Map(
        ("ID" -> m.identifier),
        ("stateNumbers" -> macroActions.map(_.internalNumber)),
        ("startStateNumber" -> macroActions.find(_.value.identifier == m.startState).get.internalNumber),
        ("macroArguments" -> m.arguments),
        ("macroVariables" -> m.localVariables)
      )
      (m_.internalNumber -> x)
    }).toMap


    val statesMap: Map[Int, Map[String, Any]] = actions.map(a_ => {
      val a: Action = a_.value

      var x: Map[String, Any] = Map(
        ("ID" -> a.identifier),
        ("label" -> a.state.label.getOrElse("")),
        ("type" -> getStateType(a.state.function)),

        // default values, might be overwritten later
        // TODO: make semantic more tolerant if default value is not given
        ("function" -> ""),
        ("functionArguments" -> Seq()),
        ("priority" -> 0),

        ("outgoingTransitionNumbers" -> transitions.filter(_.parent == a_).map(_.internalNumber))
      )

      a.state.attributes.foreach {
        case StateHasPriority(p) => {
          x += ("priority" -> p)
        }
        case StateHasAdditionalSemantics(_) => () // irrelevant for execution
      }

      a.state.function match {
        case Some(Terminate(Some(resultValue))) => {
          x += ("functionArguments" -> Seq(resultValue))
        }
        case Some(Return(Some(resultValue))) => {
          x += ("functionArguments" -> Seq(resultValue))
        }
        case Some(ModalJoin) => throw new IllegalArgumentException("ModalJoin must be transformed to ModalJoinRich first")
        case Some(t) => {
          val fNameO = getFunctionName(t)

          fNameO match {
            case Some(sName) => {
              x += ("function" -> sName)
              x += ("functionArguments" -> getFunctionArguments(t))
            }
            case None => ()
          }
        }
        case None => ()
      }

      (a_.internalNumber -> x)
    }).toMap


    val transitionsMap: Map[Int, Map[String, Any]] = transitions.map(t_ => {
      val t: Transition = t_.value
      val a2: ActionWithInternalNumber = t_.parent
      val a: Action = a2.value
      val m2: MacroWithInternalNumber = a2.parent

      val to: InternalNumberTransition = actions.filter(_.parent == m2).find(_.value.identifier == t.targetStateIdentifier).get.internalNumber

      var x: Map[String, Any] = Map(
        ("targetStateNumber" -> to),

        // default values, might be overwritten later
        ("label" -> "undefined"),
        ("type" -> "normal"),
        ("auto" -> false),
        ("hidden" -> false),
        ("timeout" -> 0)
      )

      var isNormal = true

      t.attributes.foreach {
        case TransitionIsCancel => {
          isNormal = false
          x += ("type" -> "cancel")
        }
        case TransitionIsHidden => {
          x += ("hidden" -> true)
        }
        case TransitionHasTimeout(t) => {
          isNormal = false
          x += ("type" -> "timeout")
          x += ("timeout" -> t.toSeconds)
        }
        case TransitionHasPriority(p) => {
          x += ("priority" -> p)
        }
      }

      if (isNormal) {
        a.state.function match {
          case Some(s) if s.isInstanceOf[AutoFunction] => {
            x += ("auto" -> true)
          }
          case _ => ()
        }
      }

      t.condition match {
        case Some(DoTransitionCondition(description)) => {
          x += ("label" -> description)
        }
        case Some(MessageExchangeCondition(
        messageType,
        subject,
        subjectVariable,
        subjectCount,
        contentVariable,
        senderCorrelationVariable,
        receiverCorrelationVariable,
        storeVariable,
        label
        )) => {
          x += ("msgType" -> messageType)
          x += ("subject" -> subject)
          x += ("subjectCountMin" -> subjectCount._1)
          x += ("subjectCountMax" -> subjectCount._2)
          x += ("subjectVar" -> subjectVariable.getOrElse(""))
          x += ("contentVar" -> contentVariable.getOrElse(""))
          x += ("withCorrelationVar" -> receiverCorrelationVariable.getOrElse(""))
          x += ("newCorrelationVar" -> senderCorrelationVariable.getOrElse(""))
          x += ("storeReceiverVar" -> storeVariable.getOrElse("")) // FIXME: is it really okay to use this in both?
          x += ("storeMessagesVar" -> storeVariable.getOrElse(""))
          x += ("label" -> label.getOrElse("")) // FIXME: untested!
        }
        case Some(x) => throw new Exception("Unsupported exit condition: " + x)
        case None => ()
      }

      if (!x.contains("priority")) {
        // just for equality check with old behavior
        x += ("priority" -> 0)
      }

      (t_.internalNumber -> x)
    }).toMap

    val dataMap = processAttributes.collect({
        case ProcessAgents(a) => ("agents", a)
        case ProcessData(k, v) => (k, v)
    }).toMap


    Map[String, Any](
        ("data" -> dataMap),
        ("subjects" -> subjectsMap),
        ("startSubjectIDs" -> startSubjectIDs),
        ("macros" -> macrosMap),
        ("processMacroNumbers" -> processMacroNumbers),
        ("states" -> statesMap),
        ("transitions" -> transitionsMap)
      )
  }

}
