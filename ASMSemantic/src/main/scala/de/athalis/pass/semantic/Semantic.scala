package de.athalis.pass.semantic

import de.athalis.coreasm.binding._

// scalastyle:off number.of.methods
object Semantic {
  import Typedefs._

  // general, independent
  case object aALL extends DerivedSetFunction[String]{ def id = "aALL"; }
  case object taskSetOut extends DerivedSetFunction[Map[String, Any]]{ def id = "taskSetOut"; }
  case object runningSubjects extends DerivedFunctionMapped[Set[Seq[Any]], Set[Channel]] with DerivedSetFunction[Channel]{ def id = "runningSubjects"; def mapper = _.map(Channel.create); }

  case object startAbleProcesses extends DerivedSetFunction[String]{ def id = "startAbleProcesses"; }

  // defined in ui.casm
  case class AllAllowedStates(ch: Channel) extends DerivedMapFunction[Double, Seq[Double]]{ def id = "UI_AllAllowedStates"; override def arguments = List(ch.toSeq); }
  case class AllActiveStates(ch: Channel) extends DerivedMapFunction[Double, Seq[Double]]{ def id = "UI_AllActiveStates"; override def arguments = List(ch.toSeq); }

  case class WantInput(ch: Channel, macroInstanceNumber: Int, stateNumber: Int) extends DerivedSetFunction[String]{ def id = "UI_WantInput"; override def arguments = List(ch.toSeq, macroInstanceNumber, stateNumber); }
  case class CancelTransitionNumber(processID: String, stateNumber: Int) extends DerivedFunctionMapped[Double, Int]{ def id = "UI_CancelTransitionNumber"; override def arguments = List(processID, stateNumber); def mapper = _.toInt; }

  case class MacroID(processID: String, macroNumber: Int) extends DerivedFunction[String]{ def id = "UI_MacroID"; override def arguments = List(processID, macroNumber); }

  case class StateLabel   (processID: String, stateNumber: Int) extends DerivedFunction[String]{ def id = "UI_StateLabel"; override def arguments = List(processID, stateNumber); }
  case class StateType    (processID: String, stateNumber: Int) extends DerivedFunction[String]{ def id = "UI_StateType"; override def arguments = List(processID, stateNumber); }
  case class StateFunction(processID: String, stateNumber: Int) extends DerivedFunction[String]{ def id = "UI_StateFunction"; override def arguments = List(processID, stateNumber); }

  case class TransitionName    (processID: String, transitionNumber: Int) extends DerivedFunction[String]{ def id = "UI_TransitionName"; override def arguments = List(processID, transitionNumber); }
  case class TransitionIsHidden(processID: String, transitionNumber: Int) extends DerivedFunction[Boolean]{ def id = "UI_TransitionIsHidden"; override def arguments = List(processID, transitionNumber); }
  case class targetStateLabel  (processID: String, transitionNumber: Int) extends DerivedFunction[String]{ def id = "UI_targetStateLabel"; override def arguments = List(processID, transitionNumber); }

  case class messageTypeForFirstTransition(processID: String, stateNumber: Int) extends DerivedFunction[String]{ def id = "UI_messageTypeForFirstTransition"; override def arguments = List(processID, stateNumber); }

  case class EnabledOutgoingTransitions(ch: Channel, macroInstanceNumber: Int, stateNumber: Int) extends DerivedFunctionMapped[Set[Double], Set[Int]] with DerivedSetFunction[Int]{ def id = "UI_EnabledOutgoingTransitions"; override def arguments = List(ch.toSeq, macroInstanceNumber, stateNumber); def mapper = _.map(_.toInt); }
  case class GetIPMessages(ch: Channel, macroInstanceNumber: Int, stateNumber: Int, transitionNumber: Int) extends DerivedSeqFunction[String]{ def id = "UI_GetIPMessages"; override def arguments = List(ch.toSeq, macroInstanceNumber, stateNumber, transitionNumber); }

  case class SelectAgentsProcessID(ch: Channel, macroInstanceNumber: Int, stateNumber: Int) extends DerivedFunction[String]{ def id = "UI_SelectAgentsProcessID"; override def arguments = List(ch.toSeq, macroInstanceNumber, stateNumber); }
  case class SelectAgentsSubjectID(ch: Channel, macroInstanceNumber: Int, stateNumber: Int) extends DerivedFunction[String]{ def id = "UI_SelectAgentsSubjectID"; override def arguments = List(ch.toSeq, macroInstanceNumber, stateNumber); }
  case class SelectAgentsCountMin (ch: Channel, macroInstanceNumber: Int, stateNumber: Int) extends DerivedFunctionMapped[Double, Int]{ def id = "UI_SelectAgentsCountMin"; override def arguments = List(ch.toSeq, macroInstanceNumber, stateNumber); def mapper = _.toInt; }
  case class SelectAgentsCountMax (ch: Channel, macroInstanceNumber: Int, stateNumber: Int) extends DerivedFunctionMapped[Double, Int]{ def id = "UI_SelectAgentsCountMax"; override def arguments = List(ch.toSeq, macroInstanceNumber, stateNumber); def mapper = _.toInt; }

  case class SelectionOptions(ch: Channel, macroInstanceNumber: Int, stateNumber: Int) extends DerivedSeqFunction[String]{ def id = "UI_SelectionOptions"; override def arguments = List(ch.toSeq, macroInstanceNumber, stateNumber); }
  case class SelectionMin    (ch: Channel, macroInstanceNumber: Int, stateNumber: Int) extends DerivedFunctionMapped[Double, Int]{ def id = "UI_SelectionMin"; override def arguments = List(ch.toSeq, macroInstanceNumber, stateNumber); def mapper = _.toInt; }
  case class SelectionMax    (ch: Channel, macroInstanceNumber: Int, stateNumber: Int) extends DerivedFunctionMapped[Double, Int]{ def id = "UI_SelectionMax"; override def arguments = List(ch.toSeq, macroInstanceNumber, stateNumber); def mapper = _.toInt; }

  case class Receivers(ch: Channel, macroInstanceNumber: Int, stateNumber: Int) extends DerivedFunctionMapped[Set[Seq[Any]], Set[Channel]] with DerivedSetFunction[Channel]{ def id = "UI_Receivers"; override def arguments = List(ch.toSeq, macroInstanceNumber, stateNumber); def mapper = _.map(Channel.create); }

  case class getDebugIP(ch: Channel) extends DerivedFunction[String]{ def id = "UI_getDebugIP"; override def arguments = List(ch.toSeq); }
  case class getDebugVariables(ch: Channel) extends DerivedFunction[String]{ def id = "UI_getDebugVariables"; override def arguments = List(ch.toSeq); }
  case class getMacroNumberOfMI(ch: Channel, macroInstanceNumber: Int) extends DerivedFunctionMapped[Double, Int]{ def id = "UI_getMacroNumberOfMI"; override def arguments = List(ch.toSeq, macroInstanceNumber); def mapper = _.toInt; }

  case class LoadVarForChannel(ch: Channel, macroInstanceNumber: Int, varname: String) extends DerivedSeqFunction[Any]{ def id = "UI_LoadVarForChannel"; override def arguments = List(ch.toSeq, macroInstanceNumber, varname); }
}
// scalastyle:on number.of.methods
