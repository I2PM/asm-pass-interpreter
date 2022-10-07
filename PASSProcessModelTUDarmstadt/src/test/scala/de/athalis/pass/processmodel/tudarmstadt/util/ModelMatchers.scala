package de.athalis.pass.processmodel.tudarmstadt.util

import de.athalis.pass.processmodel.PASSProcessModelCollection
import de.athalis.pass.processmodel.tudarmstadt.Types._
import de.athalis.pass.processmodel.tudarmstadt._

import org.scalatest.matchers.should.Matchers

trait ModelMatchers extends Matchers {

  /*
  private def sortedVarManActionArguments(p: Process): Process = {
    p.copy(
      macros = p.macros.map(sortedVarManActionArguments),
      subjects = p.subjects.map(sortedVarManActionArguments)
    )
  }

  private def sortedVarManActionArguments(s: Subject): Subject = {
    s match {
      case sI: FullySpecifiedSubject => sI.copy(internalBehavior = sI.internalBehavior.copy(
        mainMacro = sortedVarManActionArguments(sI.internalBehavior.mainMacro),
        additionalMacros = sI.internalBehavior.additionalMacros.map(sortedVarManActionArguments)
      ))
      case _ => s
    }
  }

  private def sortedVarManActionArguments(m: Macro): Macro = {
    m.copy(actions = m.actions.map(sortedVarManActionArguments))
  }

  private def sortedVarManActionArguments(a: Action): Action = {
    a.state.function match {
      case Some(v: VarMan) => {
        a.copy(state = a.state.copy(function = Some(sortedVarManActionArguments(v))))
      }
      case _ => a
    }
  }

  private def sortedVarManActionArguments(v: VarMan): VarMan = {
    if (Seq("concatenation", "intersection").contains(v.method)) {
      v.copy(methodArguments = v.methodArguments.asInstanceOf[Seq[VariableIdentifier]].sorted)
    }
    else {
      v
    }
  }
  */


  implicit class ProcessModelCollectionShouldWrapper(val left: PASSProcessModelCollection[Process]) {
    def shouldDeepMatch(right: PASSProcessModelCollection[Process]): Unit = {
      left.getProcessModels[Process] shouldDeepMatch right.getProcessModels[Process]
    }
  }

  implicit class ProcessModelSetShouldWrapper(val leftOrig: Set[Process]) {
    def shouldDeepMatch(rightOrig: Set[Process]): Unit = {
      val left = leftOrig //.map(sortedVarManActionArguments)
      val right = rightOrig //.map(sortedVarManActionArguments)

      val leftProcessMap: Map[ProcessIdentifier, Process] = left.map(p => (p.identifier, p)).toMap
      val rightProcessMap: Map[ProcessIdentifier, Process] = right.map(p => (p.identifier, p)).toMap

      val processKeys = rightProcessMap.keys

      withClue("ProcessIDs") {
        leftProcessMap.keys shouldBe processKeys
      }

      processKeys.foreach(k => leftProcessMap(k) shouldDeepMatch rightProcessMap(k))
    }
  }

  implicit class ProcessShouldWrapper(val left: Process) {
    def shouldDeepMatch(right: Process): Unit = withClue(s"Process '${right.identifier}' -> ") {
      left should have(
        Symbol("identifier")(right.identifier),
        Symbol("subjectToSubjectCommunication")(right.subjectToSubjectCommunication)
      )

      val leftSubjectMap: Map[SubjectIdentifier, Subject] = left.subjects.map(s => (s.identifier, s)).toMap
      val rightSubjectMap: Map[SubjectIdentifier, Subject] = right.subjects.map(s => (s.identifier, s)).toMap

      val subjectIdentifiers = rightSubjectMap.keys

      withClue("SubjectIDs") {
        leftSubjectMap.keys shouldBe subjectIdentifiers
      }

      subjectIdentifiers.foreach(k => leftSubjectMap(k) shouldDeepMatch rightSubjectMap(k))

      // NOTE: Process Macros are currently unsupported in OWL,
      //   see https://github.com/Locke/asm-pass-interpreter/issues/9

      val leftProcessMacroMap: Map[MacroIdentifier, Macro] = left.macros.map(m => (m.identifier, m)).toMap
      val rightProcessMacroMap: Map[MacroIdentifier, Macro] = right.macros.map(m => (m.identifier, m)).toMap

      val processMacroIdentifiers = rightProcessMacroMap.keys

      withClue("Process MacroIDs") {
        leftProcessMacroMap.keys shouldBe processMacroIdentifiers
      }

      processMacroIdentifiers.foreach(k => leftProcessMacroMap(k) shouldDeepMatch rightProcessMacroMap(k))
    }
  }

  implicit class SubjectShouldWrapper(val left: Subject) {
    def shouldDeepMatch(right: Subject): Unit = withClue(s"Subject '${right.identifier}' -> ") {
      left should have(
        Symbol("identifier")(right.identifier),
        Symbol("maximumInstanceRestriction")(right.maximumInstanceRestriction)
      )

      (left, right) match {
        case (l: FullySpecifiedSubject, r: FullySpecifiedSubject) => l shouldDeepMatch r
        //case (l: DefinedInterfaceSubject, r: DefinedInterfaceSubject) => ???
        //case (l: UndefinedInterfaceSubject, r: UndefinedInterfaceSubject) => ???
        case (l, r) => l shouldEqual r
      }
    }
  }

  implicit class FullySpecifiedSubjectShouldWrapper(val left: FullySpecifiedSubject) {
    def shouldDeepMatch(right: FullySpecifiedSubject): Unit = {
      left should have(
        Symbol("attributes")(right.attributes)
      )

      left.internalBehavior shouldDeepMatch right.internalBehavior
    }
  }

  implicit class InternalBehaviorShouldWrapper(val left: InternalBehavior) {
    def shouldDeepMatch(right: InternalBehavior): Unit = {
      withClue("MainMacro") {
        left.mainMacro shouldDeepMatch right.mainMacro
      }

      withClue("AdditionalMacros -> ") {
        val leftMacroMap: Map[MacroIdentifier, Macro] = left.additionalMacros.map(m => (m.identifier, m)).toMap
        val rightMacroMap: Map[MacroIdentifier, Macro] = right.additionalMacros.map(m => (m.identifier, m)).toMap

        val macroIdentifiers = rightMacroMap.keys

        withClue("MacroIDs") {
          leftMacroMap.keys shouldBe macroIdentifiers
        }

        macroIdentifiers.foreach(k => leftMacroMap(k) shouldDeepMatch rightMacroMap(k))
      }
    }
  }

  implicit class MacroShouldWrapper(val left: Macro) {
    def shouldDeepMatch(right: Macro): Unit = withClue(s"'${right.identifier}' -> ") {
      left should have(
        Symbol("identifier")(right.identifier),
        Symbol("startState")(right.startState),
        Symbol("localVariables")(right.localVariables)
      )

      // NOTE: order is not preserved in OWL, therefore parsed sorted
      left.arguments shouldBe right.arguments.sorted

      val leftActionMap: Map[MacroIdentifier, Action] = left.actions.map(a => (a.identifier, a)).toMap
      val rightActionMap: Map[MacroIdentifier, Action] = right.actions.map(a => (a.identifier, a)).toMap

      val actionIdentifiers = rightActionMap.keys

      withClue("ActionIDs") {
        leftActionMap.keys shouldBe actionIdentifiers
      }

      actionIdentifiers.foreach(k => leftActionMap(k) shouldDeepMatch rightActionMap(k))
    }
  }

  implicit class ActionShouldWrapper(val left: Action) {
    def shouldDeepMatch(right: Action): Unit = withClue(s"Action '${right.identifier}' -> ") {
      left.identifier shouldBe right.identifier

      left.state shouldDeepMatch right.state

      val leftTransitionsByTarget: Map[StateIdentifier, Set[Transition]] = left.outgoingTransitions.groupBy(_.targetStateIdentifier)
      val rightTransitionsByTarget: Map[StateIdentifier, Set[Transition]] = right.outgoingTransitions.groupBy(_.targetStateIdentifier)

      val transitionTargets = rightTransitionsByTarget.keys

      withClue("TransitionTargets") {
        leftTransitionsByTarget.keys shouldBe transitionTargets
      }

      transitionTargets.foreach(s => withClue(s"Transition to Target '$s': ") {
        leftTransitionsByTarget(s) shouldBe rightTransitionsByTarget(s)
      })
    }
  }

  implicit class StateShouldWrapper(val left: State) {
    def shouldDeepMatch(right: State): Unit = withClue(s"State '${right.label}' -> ") {
      left.copy(function = None) shouldBe right.copy(function = None)
      // NOTE: CallMacro Function does not preserve the order of arguments. Test function separately.

      right.function match {
        case Some(CallMacro(_, _)) => {
          left.function shouldBe defined
          left.function.get shouldBe a[CallMacro]

          val leftCallMacroFunction = left.function.get.asInstanceOf[CallMacro]
          val rightCallMacroFunction = right.function.get.asInstanceOf[CallMacro]

          leftCallMacroFunction.macroID shouldBe rightCallMacroFunction.macroID

          // TODO: determine the supposed order by looking into the arguments of the called Macro.
          //   For now, just make sure they have the same arguments in any order.
          //   See https://github.com/Locke/asm-pass-interpreter/issues/10
          leftCallMacroFunction.macroArguments.toSet shouldBe rightCallMacroFunction.macroArguments.toSet
        }
        case _ => left shouldBe right
      }
    }
  }

}
