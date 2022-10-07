package de.athalis.pass.processmodel.tudarmstadt

import de.athalis.pass.processmodel.PASSProcessModel

import scala.concurrent.duration.FiniteDuration

object Types {
  type ProcessIdentifier = String
  type SubjectIdentifier = String
  type MacroIdentifier = String
  type StateIdentifier = String
  type SubjectTerminationResult = String
  type VariableIdentifier = String
  type MacroExitResult = String
  type MacroArgument = String
  type FunctionName = String
  type FunctionArgument = Any
  type MessageType = String

  /**
    * (min, max) // TODO: too tightly coupled with the ASM Interpreter Semantics.
    *
    * min = 0 => exactly from subjectVariable
    * max = 0 => at least min, but unlimited
    * else: exactly this amount
    */
  type SubjectCount = (Int, Int)

  type AdditionalSemantics = String

  type AgentIdentifier = String
  //type ProcessInstanceNumber = Int
  //type Channel = (ProcessIdentifier, ProcessInstanceNumber, SubjectIdentifier, AgentIdentifier)

  type VarManMethod = String // TODO: enum / abstract class
  type VarManMethodArgument = FunctionArgument
}

import Types._


case class Process(
                    identifier: ProcessIdentifier,
                    subjects: Set[Subject],
                    subjectToSubjectCommunication: Option[Set[MessageExchanges]] = None,
                    macros: Set[Macro] = Set.empty,
                    attributes: Set[ProcessAttribute] = Set.empty
                  ) extends PASSProcessModel


trait Subject {
  def identifier: SubjectIdentifier

  def maximumInstanceRestriction: Option[Int]
}

trait InterfaceSubject extends Subject

case class DefinedInterfaceSubject(
                                    identifier: SubjectIdentifier,
                                    process: ProcessIdentifier,
                                    subject: SubjectIdentifier,
                                    maximumInstanceRestriction: Option[Int] = None
                                  ) extends InterfaceSubject

case class UndefinedInterfaceSubject(
                                      identifier: SubjectIdentifier,
                                      maximumInstanceRestriction: Option[Int] = None
                                    ) extends InterfaceSubject

case class FullySpecifiedSubject(
                            identifier: SubjectIdentifier,
                            internalBehavior: InternalBehavior,
                            maximumInstanceRestriction: Option[Int] = None,
                            attributes: Set[SubjectAttribute] = Set.empty
                          ) extends Subject

abstract class SubjectAttribute

/**
  * Limits the InputPool per (Receiver, MessageType, CorrelationID) tuple.
  * Default: unlimited if not given.
  *
  * @param size The limit of each (Receiver, MessageType, CorrelationID) tuple.
  */
case class SubjectHasInputPoolSize(size: Int) extends SubjectAttribute

case object SubjectIsStartSubject extends SubjectAttribute

case class SubjectHasAdditionalSemantics(additionalSemantics: AdditionalSemantics) extends SubjectAttribute


case class MessageExchanges(
                                 sender: SubjectIdentifier,
                                 receiver: SubjectIdentifier,
                                 messageTypes: Set[MessageType]
                               )

case class InternalBehavior(
                              mainMacro: Macro,
                              additionalMacros: Set[Macro] = Set.empty
                            )

/**
  *
  * @param identifier     Unique identifier within a [[Process]]
  * @param startState     The first State to be started in a macro instance
  * @param actions        Set of all Actions within the Macro
  * @param arguments      List of Variables in which arguments are stored
  * @param localVariables Set of Variables which are scoped to the macro instance
  */
case class Macro(
                  identifier: MacroIdentifier,
                  startState: StateIdentifier,
                  actions: Set[Action],
                  arguments: Seq[MacroArgument] = Seq.empty,
                  localVariables: Set[VariableIdentifier] = Set.empty
                )

case class Action(
                   identifier: StateIdentifier,
                   state: State,
                   outgoingTransitions: Set[Transition]
                 )

case class State(
                  label: Option[String] = None,
                  attributes: Set[StateAttribute] = Set.empty,
                  function: Option[Function] = None
               )

case class Transition(
                       targetStateIdentifier: StateIdentifier,
                       condition: Option[TransitionCondition] = None,
                       attributes: Set[TransitionAttribute] = Set.empty
                  )


/**
  * Attributes a [[Process]] can have.
  */
abstract class ProcessAttribute

/**
  * Predefined mapping of subjects and agents.
  *
  * @param mapping The mapping.
  */
case class ProcessAgents(mapping: Map[SubjectIdentifier, Set[AgentIdentifier]]) extends ProcessAttribute

/**
  * Generic / unspecified process data.
  */
case class ProcessData(key: String, value: Any) extends ProcessAttribute

/**
  * Attributes a [[State]] can have.
  */
abstract class StateAttribute

/**
  * When multiple States are active, only the ones with the highest priority can execute.
  *
  * @param priority The priority, higher values are more important.
  */
case class StateHasPriority(priority: Int) extends StateAttribute

case class StateHasAdditionalSemantics(additionalSemantics: AdditionalSemantics) extends StateAttribute


/**
  * Condition to enable an [[Transition]].
  */
sealed trait TransitionCondition

/**
  * Textual description of the condition to enable a [[Transition]] of an [[Action]].
  *
  * @param description The conditions description.
  */
case class DoTransitionCondition(description: String) extends TransitionCondition

// just an idea: case class FunctionTransitionCondition(operator: ((Any, Any)=>Boolean), value: Any) extends TransitionCondition

/**
  * Condition to enable Transitions of Actions with an Send or Receive Function.
  *
  * @note Used by [[AutoSend]], [[ManualSend]], [[AutoReceive]] and [[ManualReceive]].
  * @param messageType                 The message type.
  * @param subject                     The subject.
  * @param subjectVariable             Variable containing Channels.
  *                                     - on Send: used as preselected receivers.
  *                                     - on Receive: used to limit the senders.
  * @param subjectCount                The number of senders / receivers of a MultiSubject.
  *                                    Special cases:
  *                                    (0, 0) => all in subjectVariable
  *                                    (x, 0) => at least x
  * @param contentVariable             Variable which content should be used as message content.
  * @param senderCorrelationVariable   Variable used for the CorrelationID of the Sender
  *                                     - on Send: a new CorrelationID is created and stored in this Variable
  *                                     - on Receive: unused
  * @param receiverCorrelationVariable Variable used for the CorrelationID by the Receiver
  *                                     - on Send: Variable containing a CorrelationID created by the Receiver
  *                                     - on Receive: Variable containing a CorrelationID previously created by this Receiver in another Send as senderCorrelationVariable
  * @param storeVariable               Variable to store the "result"
  *                                     - on Send: the selected Receivers
  *                                     - on Receive: the received messages
  */
case class MessageExchangeCondition(
                                     messageType: MessageType,
                                     subject: SubjectIdentifier,
                                     subjectVariable: Option[VariableIdentifier] = None,
                                     subjectCount: SubjectCount = (1, 1),
                                     contentVariable: Option[VariableIdentifier] = None,
                                     senderCorrelationVariable: Option[VariableIdentifier] = None,
                                     receiverCorrelationVariable: Option[VariableIdentifier] = None,
                                     storeVariable: Option[VariableIdentifier] = None,
                                     label: Option[String] = None
                                   ) extends TransitionCondition

/**
  * Attributes an [[Transition]] can have.
  */
abstract class TransitionAttribute

/**
  * Aborts the Action.
  */
case object TransitionIsCancel extends TransitionAttribute

/**
  * Transition is hidden from the user and can only be called by an internal Function, e.g. used in combination with [[TransitionIsCancel]] to be activated by [[Cancel]].
  */
case object TransitionIsHidden extends TransitionAttribute

/**
  * Aborts the Action automatically after [[TransitionHasTimeout.time]].
  *
  * @param time The time.
  */
case class TransitionHasTimeout(time: FiniteDuration) extends TransitionAttribute

/**
  * Only Transitions with the highest priority can be activated.
  *
  * @param priority The priority, higher values are more important.
  */
case class TransitionHasPriority(priority: Int) extends TransitionAttribute


/**
  * A predefined Function that is executed in an [[Action]].
  */
abstract class Function

/**
  * A [[Function]] that executes the Action without user interaction, if possible.
  */
abstract class AutoFunction extends Function

/**
  * A [[Function]] that executes the Action only with user interaction.
  */
abstract class ManualFunction extends Function

/**
  * Unspecified Function. Activates a random outgoing Transition.
  */
case object Tau extends AutoFunction

/**
  * Sends a message specified by the [[MessageExchangeCondition]] of the outgoing CommunicationTransition. Waits for user confirmation before sending.
  */
case object ManualSend extends ManualFunction

/**
  * Receives a message specified by the [[MessageExchangeCondition]] of any outgoing CommunicationTransition. Waits for user confirmation before removing the message from the InputPool.
  */
case object ManualReceive extends ManualFunction

/**
  * Sends a message specified by the [[MessageExchangeCondition]] of the outgoing CommunicationTransition. When all necessary data is available (e.g. given in Variables) it is send immediately.
  */
case object AutoSend extends AutoFunction

/**
  * Receives a message specified by the [[MessageExchangeCondition]] of any outgoing CommunicationTransition and removes it from the InputPool immediately once its available.
  */
case object AutoReceive extends AutoFunction

/**
  * Creates a new macro instance and waits for its termination.
  *
  * @param macroID        The [[Macro]] to be called.
  * @param macroArguments Arguments to be filled in [[Macro.arguments]].
  */
case class CallMacro(
                      macroID: MacroIdentifier,
                      macroArguments: Seq[VariableIdentifier] = Seq.empty
                    ) extends AutoFunction

/**
  * Activates the cancel-Transition of another Action.
  */
case object Cancel extends AutoFunction

/**
  * ModalSplit is used to initiate Choice Segment Paths, all outgoing Transitions are activated and must lead to the same [[ModalJoin]] Action.
  */
case object ModalSplit extends AutoFunction

/**
  * Joins multiple paths of a [[ModalSplit]], has to be parsed to [[ModalJoinRich]].
  */
case object ModalJoin extends AutoFunction

/**
  * Joins multiple paths of a [[ModalSplit]].
  */
case class ModalJoinRich(splitCount: Int) extends AutoFunction

/**
  * Terminates the Subject. Must only be used in the mainmacro.
  * When other Actions of the macro instance are still active they are aborted.
  *
  * @param result sets a result, shall only be used for TestTransitions2.
  */
case class Terminate(result: Option[String] = None) extends AutoFunction

/**
  * Terminates the execution of the macro instance and returns to the [[CallMacro]] that started this macro instance.. Must not be used in the mainmacro.
  * When other Actions of the macro instance are still active they are aborted.
  *
  * @param result enables the [[DoTransitionCondition]] of the [[CallMacro]] that started this macro instance.
  */
case class Return(result: Option[String] = None) extends AutoFunction

/**
  * Variable Manipulation
  *
  * @param method          The variable manipulation method to be performed
  * @param methodArguments The arguments for the variable manipulation
  */
case class VarMan(method: VarManMethod, methodArguments: Seq[VarManMethodArgument]) extends AutoFunction

trait SpecificIPFunction extends AutoFunction {
  def senderSubjID: SubjectIdentifier

  def messageType: MessageType

  def correlationID: Option[VariableIdentifier]
}

case class CloseIP(
                    senderSubjID: SubjectIdentifier,
                    messageType: MessageType,
                    correlationID: Option[VariableIdentifier] = None
                  ) extends SpecificIPFunction

case class OpenIP(
                   senderSubjID: SubjectIdentifier,
                   messageType: MessageType,
                   correlationID: Option[VariableIdentifier] = None
                 ) extends SpecificIPFunction

case class IsIPEmpty(
                      senderSubjID: SubjectIdentifier,
                      messageType: MessageType,
                      correlationID: Option[VariableIdentifier] = None
                    ) extends SpecificIPFunction

case object CloseAllIPs extends AutoFunction

case object OpenAllIPs extends AutoFunction

case class SelectAgents(
                         destination: VariableIdentifier,
                         subject: SubjectIdentifier,
                         countMin: Int,
                         countMax: Int
                       ) extends ManualFunction
