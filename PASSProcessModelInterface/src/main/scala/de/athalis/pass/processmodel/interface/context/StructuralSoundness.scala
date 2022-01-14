package de.athalis.pass.processmodel.interface.context

import de.athalis.pass.processmodel.tudarmstadt._

private trait StructuralSoundnessAnalysisAction {
  def analyzeAction(m: Macro, action: Action, suffix: String): Unit

  def outgoingTransitionToString(e: Transition): String = {
    e.condition.collect({
      case DoTransitionCondition(text) => text
      case MessageExchangeCondition(messageType, subject, _, _, _, _, _, _, label) => label.getOrElse("None") + " (" + messageType + " to/from " + subject + ")"
    }).getOrElse("")
  }
}

class StructuralSoundnessAnalysis(p: Process) extends Analysis[AnalysisDone.type] {
  override def analyze(): AnalysisDone.type = {
    StructuralSoundnessAnalysis.analyzeProcess(p)
    AnalysisDone
  }
}

private object StructuralSoundnessAnalysis {
  private val stateAnalyses = Set(StateConnectionsAnalysis, MessageSubjectCountAnalysis)

  private def analyzeProcess(p: Process): Unit = {
    val jobs: Seq[(Macro, String)] =
      p.subjects.collect[(FullySpecifiedSubject, Set[Macro]), Set[(FullySpecifiedSubject, Set[Macro])]] {
        case s: FullySpecifiedSubject => (s, s.internalBehavior.additionalMacros + s.internalBehavior.mainMacro)
      }.toSeq.flatMap(sm => sm._2.map(m => (m, getSuffix(p, sm._1, m)))) ++
        p.macros.toSeq.map(m => (m, getSuffix(p, m)))

    jobs.par.foreach(t => analyzeMacro(t._1, t._2))
  }

  private def analyzeMacro(m: Macro, suffix: String): Unit = {
    stateAnalyses.foreach(analysis => {
      m.actions.foreach(a => analysis.analyzeAction(m, a, suffix))
    })
  }

  private def getSuffix(p: Process, s: Subject, m: Macro): String = {
    "macro '%s' of subject '%s' of process '%s'".format(m.identifier, s.identifier, p.identifier)
  }

  private def getSuffix(p: Process, m: Macro): String = {
    "processmacro '%s' of process '%s'".format(m.identifier, p.identifier)
  }
}

private object StateConnectionsAnalysis extends StructuralSoundnessAnalysisAction {
  override def analyzeAction(m: Macro, action: Action, suffix: String): Unit = {
    val transitions: Set[Transition] = action.outgoingTransitions
    transitions.foreach(t => {
      val x: Option[Action] = m.actions.find(_.identifier == t.targetStateIdentifier)
      if (x.isEmpty) {
        val msg = "StateConnectionsAnalysis: destination '%s' of transition '%s' of state '%s' does not exist in ".format(t.targetStateIdentifier, outgoingTransitionToString(t), action.identifier)
        throw InvalidProcessException(msg + suffix)
      }
    })
  }
}

private object MessageSubjectCountAnalysis extends StructuralSoundnessAnalysisAction {
  private def isCommunicationAction(action: Action): Boolean = {
    if (action.state.function.isDefined) {
      val s = action.state.function.get

      (s == AutoSend || s == ManualSend || s == AutoReceive || s == ManualReceive)
    }
    else false
  }

  private def isNormalTransition(t: Transition): Boolean = {
    t.attributes.forall(a => (a != TransitionIsCancel && !a.isInstanceOf[TransitionHasTimeout]))
  }

  override def analyzeAction(m: Macro, action: Action, suffix: String): Unit = {
    if (isCommunicationAction(action)) {
      val normalTransitions: Set[Transition] = action.outgoingTransitions.filter(isNormalTransition)

      normalTransitions.foreach(e => {
        e.condition match {
          case Some(parameter) => parameter match {
            case i: MessageExchangeCondition => {
              val min = i.subjectCount._1

              if (min == 0) {
                if (i.subjectVariable.isDefined) {
                  if (i.subjectVariable.get.isEmpty) {
                    throw new InternalError("did not expect a defined Option to contain an empty string" + suffix)
                  }
                  // else: this is want we want :)
                }
                else {
                  val msg = "MessageSubjectCountAnalysis: subjectVar of transition '%s' is not defined, although the `*` operator was used in state '%s' in ".format(outgoingTransitionToString(e), action.identifier)
                  throw InvalidProcessException(msg + suffix)
                }
              }
              // else: this is okay
            }
            case x => throw new InternalError("expected InteractionExitParameter " + suffix)
          }
          case x => throw new InternalError("expected some parameter " + suffix)
        }
      })
    }
  }
}
