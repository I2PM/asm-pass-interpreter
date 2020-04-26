package de.athalis.pass.ui

import de.athalis.coreasm.binding.Binding
import de.athalis.pass.semantic.Semantic
import de.athalis.pass.semantic.Typedefs._

import de.athalis.pass.ui.loading.VarLoader

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

package object definitions {

  case class MacroInstanceF(ch: Channel, mi: Int)(implicit val binding: Binding, executionContext: ExecutionContext) {
    val macroNumberF: Future[Int] = Semantic.getMacroNumberOfMI(ch, mi).loadAsync().map(_.get)
    val macroIDF: Future[String] = macroNumberF.flatMap(macroID => Semantic.MacroID(ch.processID, macroID).loadAsync()).map(_.get)

    def getMacroInstanceAsync: Future[MacroInstance] = async {
      val macroNumber = await(macroNumberF)
      val macroID = await(macroIDF)
      MacroInstance(ch, mi, macroNumber, macroID)
    }
  }

  case class ActiveStateF(miF: MacroInstanceF, stateNumber: Int)(implicit val binding: Binding, executionContext: ExecutionContext) {
    val ch: Channel = miF.ch
    val MI: Int = miF.mi

    private val stateLabelPlainF: Future[Option[String]] = Semantic.StateLabel(ch.processID, stateNumber).loadAsync()
    val stateLabelF: Future[String] = VarLoader.transformLabel(this, stateLabelPlainF)
    val stateTypeF: Future[String] = Semantic.StateType(ch.processID, stateNumber).loadAsync().map(_.get)
    val stateFunctionF: Future[String] = Semantic.StateFunction(ch.processID, stateNumber).loadAsync().map(_.get)

    def getActiveStateAsync: Future[ActiveState] = async {
      val mi = await(miF.getMacroInstanceAsync)
      val stateLabel = await(stateLabelF)
      val stateType = await(stateTypeF)
      val stateFunction = await(stateFunctionF)
      ActiveState(mi, stateNumber, stateLabel, stateType, stateFunction)
    }
  }

  case class TransitionF(stateF: ActiveStateF, transitionNumber: Int)(implicit val binding: Binding, executionContext: ExecutionContext) {
    val isHiddenF: Future[Boolean] = Semantic.TransitionIsHidden(stateF.ch.processID, transitionNumber).loadAsync().map(_.getOrElse(false))

    private val transitionNamePlainF: Future[Option[String]] = Semantic.TransitionName(stateF.ch.processID, transitionNumber).loadAsync()
    val transitionNameF: Future[String] = VarLoader.transformLabel(stateF, transitionNamePlainF)

    private val targetStateLabelPlainF: Future[Option[String]] = Semantic.targetStateLabel(stateF.ch.processID, transitionNumber).loadAsync()
    val targetStateLabelF: Future[String] = VarLoader.transformLabel(stateF, targetStateLabelPlainF)

    def getTransitionAsync: Future[Transition] = async {
      val state = await(stateF.getActiveStateAsync)
      val isHidden = await(isHiddenF)
      val transitionName = await(transitionNameF)
      val targetStateLabel = await(targetStateLabelF)
      Transition(state, transitionNumber, isHidden, transitionName, targetStateLabel)
    }
  }

}
