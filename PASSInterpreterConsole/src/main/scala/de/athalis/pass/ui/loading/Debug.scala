package de.athalis.pass.ui.loading

import de.athalis.coreasm.binding.Binding

import de.athalis.pass.processmodel.tudarmstadt.Types.MacroIdentifier
import de.athalis.pass.semantic.Semantic
import de.athalis.pass.semantic.Typedefs._
import de.athalis.pass.ui.definitions._
import de.athalis.pass.ui.loading.ActivityLoader.mapToActiveStates

import akka.util.Timeout

import java.lang.System.{lineSeparator => EOL}

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.parallel.mutable.ParArray
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object Debug {
  private val EOL2: String = EOL + EOL

  def getGlobalState()(implicit executionContext: ExecutionContext, timeout: Timeout, binding: Binding): Future[String] = async {
    val runningSubjects: Set[Channel] = await(Semantic.runningSubjects.loadAndGetAsync())

    val out: Set[String] = await(Future.sequence(ParArray.handoff(runningSubjects.toArray).map(getChannelState).seq.toSet))

    out.mkString(EOL2)
  }

  private def getChannelState(ch: Channel)(implicit executionContext: ExecutionContext, timeout: Timeout, binding: Binding): Future[String] = async {
    val allActiveStatesF: Future[Set[ActiveStateF]] = Semantic.AllActiveStates(ch).loadAndGetAsync().map(m => mapToActiveStates(ch, m))
    val ipF = Semantic.getDebugIP(ch).loadAndGetOrElseAsync("")
    val variablesF = Semantic.getDebugVariables(ch).loadAndGetOrElseAsync("")

    val allActiveStatesF2: Future[Set[ActiveState]] = allActiveStatesF.map(f => Future.sequence(f.map(_.getActiveStateAsync))).flatten

    val allActiveStates: Set[ActiveState] = await(allActiveStatesF2)

    val allMIs: Set[MacroInstance] = allActiveStates.map(_.mi)

    val miStatesMap: Map[RuntimeMacroInstanceNumber, String] = allMIs.map(mi => {(mi.macroInstanceNumber, getMIState(mi, allActiveStates.filter(_.mi == mi)))}).toMap

    val miStates: String = miStatesMap.toSeq.sortBy(_._1).map(_._2).mkString(EOL)

    val ip = await(ipF)
    val variables = await(variablesF)

    s"""==== Channel: $ch ====
       |== IP
       |$ip
       |== Variables
       |$variables
       |== Active Macros
       |$miStates""".stripMargin
  }

  private def getMIState(mi: MacroInstance, states: Set[ActiveState]): String = {
    val statesPrettyS: Set[String] = states.map(getStateState)

    val macroID: MacroIdentifier = mi.macroID

    // TODO: sorted?
    val statesPretty = statesPrettyS.mkString(EOL)

    // NOTE: no %d formatting here for macroInstanceNumber, so that output can be shared to people with different locale
    f"""= Macro: $macroID%s (MI: ${mi.macroInstanceNumber}%s)
       |$statesPretty%s
     """.stripMargin
  }

  private def getStateState(state: ActiveState): String = {
    val stateLabel = state.stateLabel

    val stateType = state.stateType match {
      case "action" => state.stateFunction
      case stateType => stateType
    }

    s"'$stateLabel' ($stateType)$EOL"
  }
}
