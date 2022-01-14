package de.athalis.pass.ui.loading

import de.athalis.coreasm.binding.Binding

import de.athalis.pass.processmodel.tudarmstadt.Types.MessageType
import de.athalis.pass.processmodel.tudarmstadt.Types.ProcessIdentifier
import de.athalis.pass.processmodel.tudarmstadt.Types.SubjectIdentifier
import de.athalis.pass.semantic.Activities._
import de.athalis.pass.semantic.Helper.TaskMap
import de.athalis.pass.semantic.Semantic
import de.athalis.pass.semantic.Typedefs._
import de.athalis.pass.ui.definitions._

import scala.async.Async._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object ActivityLoader {
  def mapToActiveStates(ch: Channel, m: Map[RuntimeMacroInstanceNumber, Set[RuntimeStateNumber]])(implicit binding: Binding, executor: scala.concurrent.ExecutionContext): Set[ActiveStateF] = {
    m.map(x => {
      val macroInstanceNumber: RuntimeMacroInstanceNumber = x._1
      val stateNumbers: Set[RuntimeStateNumber] = x._2
      val macroInstanceF = MacroInstanceF(ch, macroInstanceNumber)
      val activeStates: Set[ActiveStateF] = stateNumbers.map(stateNumber => ActiveStateF(macroInstanceF, stateNumber))
      activeStates
    }).toSet.flatten
  }
}

class ActivityLoader()(implicit executionContext: ExecutionContext, binding: Binding) {
  import ActivityLoader._

  def loadAllAvailableActivitiesAsync(runningSubjectsF: Future[Set[Channel]]): Future[Set[PASSActivity[_ <: PASSActivityInput]]] = async {
    val taskSetActivitiesF = loadTaskSetActivitiesAsync()

    val runningSubjects: Set[Channel] = await(runningSubjectsF)

    val subjectActivitiesF: Set[Future[Set[PASSActivity[_ <: PASSActivityInput]]]] = runningSubjects.par.map(loadAvailableActivitiesAsync).seq

    val subjectActivities = await(Future.sequence(subjectActivitiesF))

    val taskSetActivities = await(taskSetActivitiesF)

    taskSetActivities ++ subjectActivities.flatten
  }


  private def loadAvailableActivitiesAsync(channel: Channel): Future[Set[PASSActivity[_ <: PASSActivityInput]]] = async {
    val allowedStatesPerMI: Map[RuntimeMacroInstanceNumber, Set[RuntimeStateNumber]] = await(Semantic.AllAllowedStates(channel).loadAndGetAsync())
    val allowedStates: Set[ActiveStateF] = mapToActiveStates(channel, allowedStatesPerMI)

    val availableActivitiesF: Set[Future[Set[PASSActivity[_ <: PASSActivityInput]]]] = allowedStates.par.map(loadAvailableActivitiesAsync).seq

    val availableActivities: Set[Set[PASSActivity[_ <: PASSActivityInput]]] = await(Future.sequence(availableActivitiesF))

    availableActivities.flatten
  }

  private def loadAvailableActivitiesAsync(currentState: ActiveStateF): Future[Set[PASSActivity[_ <: PASSActivityInput]]] = async {
    val inputF: Future[Set[PASSActivity[_ <: PASSActivityInput]]] = loadInputAsync(currentState)
    val cancelActivityF: Future[Option[PASSActivity[PASSActivityInputUnit.type]]] = loadCancelAsync(currentState)

    (await(inputF) ++ await(cancelActivityF))
  }

  private def loadInputAsync(currentState: ActiveStateF): Future[Set[PASSActivity[_ <: PASSActivityInput]]] = async {
    val wantInput = await(Semantic.WantInput(currentState.ch, currentState.MI, currentState.stateNumber).loadAndGetAsync())

    val transitionDecisionF: Future[Set[PASSActivity[_ <: PASSActivityInput]]] = if (wantInput.contains("TransitionDecision")) {
      loadTransitionDecisionAsync(currentState)
    } else Future.successful(Set.empty)

    val messageContentDecisionF: Future[Set[PASSActivity[_ <: PASSActivityInput]]] = if (wantInput.contains("MessageContentDecision")) {
      loadMessageContentDecisionAsync(currentState).map(Set(_))
    } else Future.successful(Set.empty)

    val selectionDecisionF: Future[Set[PASSActivity[_ <: PASSActivityInput]]] = if (wantInput.contains("SelectionDecision")) {
      loadSelectionDecisionAsync(currentState).map(Set(_))
    } else Future.successful(Set.empty)

    val selectAgentsDecisionF: Future[Set[PASSActivity[_ <: PASSActivityInput]]] = if (wantInput.contains("SelectAgentsDecision")) {
      loadSelectAgentsDecisionAsync(currentState).map(Set(_))
    } else Future.successful(Set.empty)

    val activitiesSet = Set(transitionDecisionF, messageContentDecisionF, selectionDecisionF, selectAgentsDecisionF)

    val activities: Set[Set[PASSActivity[_ <: PASSActivityInput]]] = await(Future.sequence(activitiesSet))
    activities.flatten
  }

  private def loadTaskSetActivitiesAsync(): Future[Set[PASSActivity[_ <: PASSActivityInput]]] = async {
    val taskSet: Set[TaskMap] = await(Semantic.taskSetOut.loadAndGetAsync())
    getTaskSetActivities(taskSet)
  }

  private def getTaskSetActivities(taskSet: Set[TaskMap]): Set[PASSActivity[_ <: PASSActivityInput]] = {
    taskSet.map(task => {
      val taskType: String = task("task").asInstanceOf[String]

      taskType match {
        case "StartSubject" => {
          val processModelID: ProcessIdentifier = task("processModelID").asInstanceOf[String]
          val pi: RuntimeProcessInstanceNumber  = task("PI").asInstanceOf[Double].toInt
          val subjectID: SubjectIdentifier      = task("subjectID").asInstanceOf[String]

          StartSubject(task, processModelID, pi, subjectID)
        }
      }
    })
  }

  private def toCancelDecision(transition: Transition): Option[PASSActivity[PASSActivityInputUnit.type]] = {
    val transitionIsHidden: Boolean = transition.isHidden
    if (transitionIsHidden) {
      None
    }
    else {
      Some(CancelDecision(transition))
    }
  }

  private def loadCancelAsync(currentState: ActiveStateF): Future[Option[PASSActivity[PASSActivityInputUnit.type]]] = async {
    val transitionNumber: Option[RuntimeTransitionNumber] = await(Semantic.CancelTransitionNumber(currentState.ch.processModelID, currentState.stateNumber).loadAsync())

    if (transitionNumber.isEmpty) {
      None
    }
    else {
      val transitionF = TransitionF(currentState, transitionNumber.get)
      val transition: Transition = await(transitionF.getTransitionAsync)

      toCancelDecision(transition)
    }
  }

  private def toTransitionDecisionAsync(currentState: ActiveStateF, transitionNumber: RuntimeTransitionNumber): Future[PASSActivity[PASSActivityInputUnit.type]] = async {
    val transitionF = TransitionF(currentState, transitionNumber)

    val stateType: String = await(currentState.stateTypeF)

    if (stateType == "receive") {
      val messagesLocation = Semantic.GetIPMessages(currentState.ch, currentState.MI, currentState.stateNumber, transitionNumber)
      val messages = await(messagesLocation.loadAndGetAsync())
      val transition: Transition = await(transitionF.getTransitionAsync)
      CommunicationTransitionDecision(transition, messages)
    }
    else {
      val transition = await(transitionF.getTransitionAsync)
      TransitionDecision(transition)
    }
  }

  private def loadTransitionDecisionAsync(currentState: ActiveStateF): Future[Set[PASSActivity[_ <: PASSActivityInput]]] = async {
    val outgoingTransitionNumbers: Set[RuntimeTransitionNumber] = await(Semantic.EnabledOutgoingTransitions(currentState.ch, currentState.MI, currentState.stateNumber).loadAndGetAsync())

    val x: Set[Future[PASSActivity[_ <: PASSActivityInput]]] = outgoingTransitionNumbers.map(e => toTransitionDecisionAsync(currentState, e))

    val activities: Future[Set[PASSActivity[_ <: PASSActivityInput]]] = Future.sequence(x)
    await(activities)
  }

  private def loadMessageContentDecisionAsync(currentState: ActiveStateF): Future[PASSActivity[PASSActivityInputMessageContent]] = async {
    val messageTypeOF = Semantic.messageTypeForFirstTransition(currentState.ch.processModelID, currentState.stateNumber).loadAsync()
    val receiversF = Semantic.Receivers(currentState.ch, currentState.MI, currentState.stateNumber).loadAndGetAsync()

    val state: ActiveState = await(currentState.getActiveStateAsync)
    val messageType: MessageType = await(messageTypeOF).get
    val receivers: Set[Channel] = await(receiversF)

    MessageContentDecision(state, messageType, receivers)
  }

  private def loadSelectionDecisionAsync(currentState: ActiveStateF): Future[PASSActivity[PASSActivityInputSelection]] = async {
    val optionsF = Semantic.SelectionOptions(currentState.ch, currentState.MI, currentState.stateNumber).loadAndGetAsync()
    val minF     = Semantic.SelectionMin    (currentState.ch, currentState.MI, currentState.stateNumber).loadAsync().map(_.get)
    val maxF     = Semantic.SelectionMax    (currentState.ch, currentState.MI, currentState.stateNumber).loadAsync().map(_.get)

    val state   = await(currentState.getActiveStateAsync)
    val options = await(optionsF)
    val min     = await(minF)
    val max     = await(maxF)

    SelectionDecision(state, options, min, max)
  }

  private def loadSelectAgentsDecisionAsync(currentState: ActiveStateF): Future[PASSActivity[PASSActivityInputAgents]] = async {
    val processModelIDF = Semantic.SelectAgentsProcessModelID(currentState.ch, currentState.MI, currentState.stateNumber).loadAsync().map(_.get)
    val subjectIDF      = Semantic.SelectAgentsSubjectID     (currentState.ch, currentState.MI, currentState.stateNumber).loadAsync().map(_.get)
    val countMinF       = Semantic.SelectAgentsCountMin      (currentState.ch, currentState.MI, currentState.stateNumber).loadAsync().map(_.get)
    val countMaxF       = Semantic.SelectAgentsCountMax      (currentState.ch, currentState.MI, currentState.stateNumber).loadAsync().map(_.get)

    val state          = await(currentState.getActiveStateAsync)
    val processModelID = await(processModelIDF)
    val subjectID      = await(subjectIDF)
    val countMin       = await(countMinF)
    val countMax       = await(countMaxF)

    SelectAgents(state, processModelID, subjectID, countMin, countMax)
  }
}
