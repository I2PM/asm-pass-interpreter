package de.athalis.pass.processutil.context

import de.athalis.pass.model.TUDarmstadtModel._
import de.athalis.pass.model.TUDarmstadtModel.Types._

case class ModalJoinAnalysisResult(joinCounts: Map[ModalJoinAnalysis.SubjectIDOptionMacroID, ModalJoinAnalysis.StateIDJoinCount]) extends AnalysisResult

object ModalJoinAnalysis {
  type SubjectIDOptionMacroID = (Option[SubjectIdentifier], MacroIdentifier)
  type StateIDJoinCount = Map[StateIdentifier, Int]
}

class ModalJoinAnalysis(val p: Process) extends Analysis[ModalJoinAnalysisResult] {
  import ModalJoinAnalysis._

  override def analyze(): ModalJoinAnalysisResult = {
    var results: Seq[(SubjectIDOptionMacroID, StateIDJoinCount)] = Seq.empty

    results ++= p.macros.map(m => ((None, m.identifier) -> analyze(m)))
    results ++= p.subjects.collect({
      case s: InternalSubject => (s.internalBehavior.mainMacro +: s.internalBehavior.additionalMacros.toSeq).map(m => ((Some(s.identifier), m.identifier) -> analyze(m)))
    }).flatten

    ModalJoinAnalysisResult(results.toMap)
  }

  private def analyze(m: Macro): ModalJoinAnalysis.StateIDJoinCount = {
    new ModalJoinAnalysisMacro(m).analyze()
  }
}

private class ModalJoinAnalysisMacro(m: Macro) {
  import ModalJoinAnalysis._

  private val startAction: Action = m.actions.find(_.identifier == m.startState).get

  // temporary for analysis
  private var numSplits = Map.empty[StateIdentifier, Int]
  private var splitStates = Map.empty[StateIdentifier, Option[StateIdentifier]]

  // result of analysis, combined previous two maps
  private var joinStates: StateIDJoinCount = Map.empty

  def analyze(): StateIDJoinCount = {
    analyze(startAction, Nil)
    joinStates
  }

  private def analyze(action: Action, splitStack: List[StateIdentifier]): Unit = {
    val splitState: Option[StateIdentifier] = splitStack.headOption
    val id: StateIdentifier = action.identifier

    if (splitStates.contains(id)) {
      val expectedSplitState: Option[StateIdentifier] = splitStates.get(id).flatten
      if (splitState != expectedSplitState) {
        throw InvalidProcessException("ModalJoin: different splits leading to state '" + id + "': '" + splitState + "' and '" + expectedSplitState + "'")
      }
      // else: everything ok, already visited, nothing to do
    }
    else {
      splitStates += (id -> splitState)

      val transitions: Set[Transition] = action.outgoingTransitions
      val nextActions: Set[Action] = transitions.map(e => m.actions.find(_.identifier == e.targetStateIdentifier).get)

      var nextSplitStack: List[StateIdentifier] = splitStack

      action.state.function match {
        case Some(ModalSplit) => {
          numSplits += (id -> transitions.size)

          nextSplitStack = id :: splitStack
        }
        case Some(ModalJoin) => {
          if (splitState.isEmpty) {
            throw InvalidProcessException("ModalJoin: join without split: '" + id + "'")
          }
          else {
            joinStates += (id -> numSplits(splitState.get))

            nextSplitStack = splitStack.tail
          }
        }
        case _ => ()
      }

      for (x <- nextActions) {
        analyze(x, nextSplitStack)
      }
    }
  }
}

object ModalJoinTransformation extends Transformation {
  import ModalJoinAnalysis._

  override def transform(process: Process, analysisResults: Seq[AnalysisResult]): Process = {
    val modalJoinAnalysisResults: Seq[ModalJoinAnalysisResult] = analysisResults.collect({
      case mj: ModalJoinAnalysisResult => mj
    })

    if (modalJoinAnalysisResults.size != 1) {
      throw new Exception("expected one ModalJoinAnalysisResult, got: " + modalJoinAnalysisResults.size)
    }

    transformProcess(process, modalJoinAnalysisResults.head)
  }

  private def transformProcess(process: Process, joinAnalysisResult: ModalJoinAnalysisResult): Process = {
    process.copy(
      macros = process.macros.map(m => {
        transformMacro(m, None, joinAnalysisResult)
      }),
      subjects = process.subjects.map {
        case si: InternalSubject => {
          val mainMacro = transformMacro(si.internalBehavior.mainMacro, Some(si.identifier), joinAnalysisResult)
          val additionalMacros = si.internalBehavior.additionalMacros.map(m => transformMacro(m, Some(si.identifier), joinAnalysisResult))
          si.copy(internalBehavior = InternalBehavior(mainMacro, additionalMacros))
        }
        case x => x
      }
    )
  }

  private def transformMacro(m: Macro, subjectIDO: Option[SubjectIdentifier], joinAnalysisResult: ModalJoinAnalysisResult): Macro = {
    m.copy(actions = m.actions.map(action => transformAction(action, (subjectIDO, m.identifier), joinAnalysisResult)))
  }

  private def transformAction(action: Action, key: SubjectIDOptionMacroID, joinAnalysisResult: ModalJoinAnalysisResult): Action = {
    action.state.function match {
      case Some(ModalJoin) => {
        val joinCountMacro: StateIDJoinCount = joinAnalysisResult.joinCounts(key)
        val joinCount: Int = joinCountMacro(action.identifier)
        action.copy(state = action.state.copy(function = Some(ModalJoinRich(joinCount))))
      }
      case x => action
    }
  }
}
