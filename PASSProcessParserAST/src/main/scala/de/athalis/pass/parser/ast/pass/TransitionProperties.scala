package de.athalis.pass.parser.ast.pass

import de.athalis.pass.parser.ast._


case class CountMinMax(minO: Option[Int], maxO: Option[Int]) extends CustomNode {
  if (minO.isDefined && minO.get < 1) throw new IllegalArgumentException("min must be at least 1 if given")
  if (maxO.isDefined && maxO.get < 1) throw new IllegalArgumentException("max must be at least 1 if given")

  def min: Int = minO.getOrElse(0)
  def max: Int = maxO.getOrElse(0)
}


sealed trait TransitionProperty extends CustomNode

case object TransitionPropertyAuto extends TransitionProperty
case object TransitionPropertyHidden extends TransitionProperty
case class TransitionPropertyTimeout(timeout: Int) extends TransitionProperty
case object TransitionPropertyCancel extends TransitionProperty
case class TransitionPropertyPriority(priority: Int) extends TransitionProperty


sealed trait CommunicationTransitionProperty extends CustomNode

case class TransitionPropertyMsgTypeWithCorrelation(msgType: String, correlation: Option[String]) extends CommunicationTransitionProperty
// TODO: as both have the same arguments now -> combine to single case?
case class TransitionPropertyMsgToSubj(msgTypeWithCorrelation: TransitionPropertyMsgTypeWithCorrelation, subject: String, inVar: Option[String], subjectCount: Option[CountMinMax]) extends CommunicationTransitionProperty
case class TransitionPropertyMsgFromSubj(msgTypeWithCorrelation: TransitionPropertyMsgTypeWithCorrelation, subject: String, inVar: Option[String], subjectCount: Option[CountMinMax]) extends CommunicationTransitionProperty

case class TransitionPropertyWithContentOf(x: String) extends CommunicationTransitionProperty
case class TransitionPropertyStoreMessagesIn(x: String) extends CommunicationTransitionProperty
case class TransitionPropertyStoreReceiverIn(x: String) extends CommunicationTransitionProperty
case class TransitionPropertyWithNewCorrelation(x: String) extends CommunicationTransitionProperty
