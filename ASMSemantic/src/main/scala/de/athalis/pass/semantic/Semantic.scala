package de.athalis.pass.semantic

import de.athalis.coreasm.binding._

import de.athalis.pass.processmodel.tudarmstadt.Types.AgentIdentifier
import de.athalis.pass.processmodel.tudarmstadt.Types.FunctionName
import de.athalis.pass.processmodel.tudarmstadt.Types.MacroIdentifier
import de.athalis.pass.processmodel.tudarmstadt.Types.MessageType
import de.athalis.pass.processmodel.tudarmstadt.Types.ProcessIdentifier
import de.athalis.pass.processmodel.tudarmstadt.Types.SubjectIdentifier
import de.athalis.pass.processmodel.tudarmstadt.Types.VariableIdentifier
import de.athalis.pass.semantic.Helper.TaskMap

// scalastyle:off number.of.methods
object Semantic {
  import Typedefs._

  // general, independent
  val aALL = ASMFunction.gettableSet[AgentIdentifier]("aALL")
  val taskSetIn  = ASMFunction.gettableSet[TaskMap]("taskSetIn")
  val taskSetOut = ASMFunction.gettableSet[TaskMap]("taskSetOut")
  val runningSubjects = ASMFunction.gettableSet[Seq[Any], Channel]("runningSubjects", Nil, _.map(Channel.create))

  val startAbleProcessModels = ASMFunction.gettableSet[ProcessIdentifier]("startAbleProcessModels")

  // defined in ui.casm
  def AllAllowedStates(ch: Channel) = ASMFunction.gettableMap[Double, Seq[Double], RuntimeMacroInstanceNumber, Set[RuntimeStateNumber]](id = "UI_AllAllowedStates", arguments = List(ch.toSeq), mapper = (m => { m.map(t => (t._1.toInt, t._2.map(_.toInt).toSet)) }) )
  def AllActiveStates (ch: Channel) = ASMFunction.gettableMap[Double, Seq[Double], RuntimeMacroInstanceNumber, Set[RuntimeStateNumber]](id = "UI_AllActiveStates",  arguments = List(ch.toSeq), mapper = (m => { m.map(t => (t._1.toInt, t._2.map(_.toInt).toSet)) }) )

  def WantInput(ch: Channel, macroInstanceNumber: RuntimeMacroNumber, stateNumber: RuntimeStateNumber)  = ASMFunction.gettableSet[String](id = "UI_WantInput", arguments = List(ch.toSeq, macroInstanceNumber, stateNumber))
  def CancelTransitionNumber(processModelID: ProcessIdentifier, stateNumber: RuntimeStateNumber) = ASMFunction.loadableInt[RuntimeTransitionNumber](id = "UI_CancelTransitionNumber", arguments = List(processModelID, stateNumber))

  def MacroID(processModelID: ProcessIdentifier, macroNumber: RuntimeMacroNumber) = ASMFunction.loadable[MacroIdentifier](id = "UI_MacroID", arguments = List(processModelID, macroNumber))

  def StateLabel   (processModelID: ProcessIdentifier, stateNumber: RuntimeStateNumber) = ASMFunction.loadable[String]      (id = "UI_StateLabel",    arguments = List(processModelID, stateNumber))
  def StateType    (processModelID: ProcessIdentifier, stateNumber: RuntimeStateNumber) = ASMFunction.loadable[String]      (id = "UI_StateType",     arguments = List(processModelID, stateNumber))
  def StateFunction(processModelID: ProcessIdentifier, stateNumber: RuntimeStateNumber) = ASMFunction.loadable[FunctionName](id = "UI_StateFunction", arguments = List(processModelID, stateNumber))

  def TransitionLabel   (processModelID: ProcessIdentifier, transitionNumber: RuntimeTransitionNumber) = ASMFunction.loadable[String] (id = "UI_TransitionLabel",    arguments = List(processModelID, transitionNumber))
  def TransitionIsHidden(processModelID: ProcessIdentifier, transitionNumber: RuntimeTransitionNumber) = ASMFunction.loadable[Boolean](id = "UI_TransitionIsHidden", arguments = List(processModelID, transitionNumber))
  def targetStateLabel  (processModelID: ProcessIdentifier, transitionNumber: RuntimeTransitionNumber) = ASMFunction.loadable[String] (id = "UI_targetStateLabel",   arguments = List(processModelID, transitionNumber))

  def messageTypeForFirstTransition(processModelID: ProcessIdentifier, stateNumber: RuntimeStateNumber) = ASMFunction.loadable[MessageType](id = "UI_messageTypeForFirstTransition", arguments = List(processModelID, stateNumber))

  def EnabledOutgoingTransitions(ch: Channel, macroInstanceNumber: RuntimeMacroInstanceNumber, stateNumber: RuntimeStateNumber) = ASMFunction.gettableSet[Double, RuntimeTransitionNumber](id = "UI_EnabledOutgoingTransitions", arguments = List(ch.toSeq, macroInstanceNumber, stateNumber), mapper = _.map(_.toInt))
  def GetIPMessages(ch: Channel, macroInstanceNumber: RuntimeMacroInstanceNumber, stateNumber: RuntimeStateNumber, transitionNumber: RuntimeTransitionNumber) = ASMFunction.gettableSeq[String](id = "UI_GetIPMessages", arguments = List(ch.toSeq, macroInstanceNumber, stateNumber, transitionNumber))

  def SelectAgentsProcessModelID(ch: Channel, macroInstanceNumber: RuntimeMacroInstanceNumber, stateNumber: RuntimeStateNumber) = ASMFunction.loadable[ProcessIdentifier](id = "UI_SelectAgentsProcessModelID", arguments = List(ch.toSeq, macroInstanceNumber, stateNumber))
  def SelectAgentsSubjectID     (ch: Channel, macroInstanceNumber: RuntimeMacroInstanceNumber, stateNumber: RuntimeStateNumber) = ASMFunction.loadable[SubjectIdentifier](id = "UI_SelectAgentsSubjectID",      arguments = List(ch.toSeq, macroInstanceNumber, stateNumber))
  def SelectAgentsCountMin      (ch: Channel, macroInstanceNumber: RuntimeMacroInstanceNumber, stateNumber: RuntimeStateNumber) = ASMFunction.loadableInt                (id = "UI_SelectAgentsCountMin",       arguments = List(ch.toSeq, macroInstanceNumber, stateNumber))
  def SelectAgentsCountMax      (ch: Channel, macroInstanceNumber: RuntimeMacroInstanceNumber, stateNumber: RuntimeStateNumber) = ASMFunction.loadableInt                (id = "UI_SelectAgentsCountMax",       arguments = List(ch.toSeq, macroInstanceNumber, stateNumber))

  def SelectionOptions(ch: Channel, macroInstanceNumber: RuntimeMacroInstanceNumber, stateNumber: RuntimeStateNumber) = ASMFunction.gettableSeq[String](id = "UI_SelectionOptions", arguments = List(ch.toSeq, macroInstanceNumber, stateNumber))
  def SelectionMin    (ch: Channel, macroInstanceNumber: RuntimeMacroInstanceNumber, stateNumber: RuntimeStateNumber) = ASMFunction.loadableInt[Int]   (id = "UI_SelectionMin",     arguments = List(ch.toSeq, macroInstanceNumber, stateNumber))
  def SelectionMax    (ch: Channel, macroInstanceNumber: RuntimeMacroInstanceNumber, stateNumber: RuntimeStateNumber) = ASMFunction.loadableInt[Int]   (id = "UI_SelectionMax",     arguments = List(ch.toSeq, macroInstanceNumber, stateNumber))

  def Receivers(ch: Channel, macroInstanceNumber: RuntimeMacroInstanceNumber, stateNumber: RuntimeStateNumber) = ASMFunction.gettableSet[Seq[Any], Channel](id = "UI_Receivers", arguments = List(ch.toSeq, macroInstanceNumber, stateNumber), mapper = _.map(Channel.create))

  def getDebugIP       (ch: Channel) = ASMFunction.loadable[String](id = "UI_getDebugIP",        arguments = List(ch.toSeq))
  def getDebugVariables(ch: Channel) = ASMFunction.loadable[String](id = "UI_getDebugVariables", arguments = List(ch.toSeq))
  def getMacroNumberOfMI(ch: Channel, macroInstanceNumber: RuntimeMacroInstanceNumber) = ASMFunction.loadableInt[RuntimeMacroNumber](id = "UI_getMacroNumberOfMI", arguments = List(ch.toSeq, macroInstanceNumber))

  def LoadVarForChannel(ch: Channel, macroInstanceNumber: RuntimeMacroInstanceNumber, varname: VariableIdentifier) = ASMFunction.gettableSeq[Any](id = "UI_LoadVarForChannel", arguments = List(ch.toSeq, macroInstanceNumber, varname))
}
// scalastyle:on number.of.methods
