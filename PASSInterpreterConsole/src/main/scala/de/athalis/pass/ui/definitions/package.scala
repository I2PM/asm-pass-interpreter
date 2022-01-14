package de.athalis.pass.ui

import de.athalis.coreasm.binding.Binding

import de.athalis.pass.processmodel.tudarmstadt.Types.FunctionName
import de.athalis.pass.processmodel.tudarmstadt.Types.MacroIdentifier
import de.athalis.pass.semantic.Semantic
import de.athalis.pass.semantic.Typedefs._
import de.athalis.pass.ui.loading.VarLoader

import scala.async.Async._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

package object definitions {

  case class MacroInstanceF(ch: Channel, mi: RuntimeMacroInstanceNumber)(implicit val binding: Binding, executionContext: ExecutionContext) {
    val macroNumberF: Future[RuntimeMacroNumber] = Semantic.getMacroNumberOfMI(ch, mi).loadAsync().map(_.get)
    val macroIDF: Future[MacroIdentifier] = macroNumberF.flatMap(macroID => Semantic.MacroID(ch.processModelID, macroID).loadAsync()).map(_.get)

    def getMacroInstanceAsync: Future[MacroInstance] = async {
      val macroNumber: RuntimeMacroNumber = await(macroNumberF)
      val macroID: MacroIdentifier = await(macroIDF)
      MacroInstance(ch, mi, macroNumber, macroID)
    }
  }

  case class ActiveStateF(miF: MacroInstanceF, stateNumber: RuntimeStateNumber)(implicit val binding: Binding, executionContext: ExecutionContext) {
    val ch: Channel = miF.ch
    val MI: RuntimeMacroInstanceNumber = miF.mi

    private val stateLabelPlainF: Future[Option[String]] = Semantic.StateLabel(ch.processModelID, stateNumber).loadAsync()
    val stateLabelF: Future[String] = VarLoader.transformLabel(this, stateLabelPlainF)
    val stateTypeF: Future[String] = Semantic.StateType(ch.processModelID, stateNumber).loadAsync().map(_.get)
    val stateFunctionF: Future[FunctionName] = Semantic.StateFunction(ch.processModelID, stateNumber).loadAsync().map(_.get)

    def getActiveStateAsync: Future[ActiveState] = async {
      val mi = await(miF.getMacroInstanceAsync)
      val stateLabel = await(stateLabelF)
      val stateType = await(stateTypeF)
      val stateFunction = await(stateFunctionF)
      ActiveState(mi, stateNumber, stateLabel, stateType, stateFunction)
    }
  }

  case class TransitionF(stateF: ActiveStateF, transitionNumber: RuntimeTransitionNumber)(implicit val binding: Binding, executionContext: ExecutionContext) {
    val isHiddenF: Future[Boolean] = Semantic.TransitionIsHidden(stateF.ch.processModelID, transitionNumber).loadAsync().map(_.getOrElse(false))

    private val transitionLabelPlainF: Future[Option[String]] = Semantic.TransitionLabel(stateF.ch.processModelID, transitionNumber).loadAsync()
    val transitionLabelF: Future[String] = VarLoader.transformLabel(stateF, transitionLabelPlainF)

    private val targetStateLabelPlainF: Future[Option[String]] = Semantic.targetStateLabel(stateF.ch.processModelID, transitionNumber).loadAsync()
    val targetStateLabelF: Future[String] = VarLoader.transformLabel(stateF, targetStateLabelPlainF)

    def getTransitionAsync: Future[Transition] = async {
      val state = await(stateF.getActiveStateAsync)
      val isHidden = await(isHiddenF)
      val transitionLabel = await(transitionLabelF)
      val targetStateLabel = await(targetStateLabelF)
      Transition(state, transitionNumber, isHidden, transitionLabel, targetStateLabel)
    }
  }

}
