package de.athalis.pass.semantic

import de.athalis.pass.processmodel.tudarmstadt.Types.AgentIdentifier
import de.athalis.pass.processmodel.tudarmstadt.Types.FunctionName
import de.athalis.pass.processmodel.tudarmstadt.Types.MacroIdentifier
import de.athalis.pass.processmodel.tudarmstadt.Types.ProcessIdentifier
import de.athalis.pass.processmodel.tudarmstadt.Types.SubjectIdentifier

object Typedefs {

  type RuntimeProcessInstanceNumber = Int

  type RuntimeMacroNumber = Int
  type RuntimeMacroInstanceNumber = Int

  type RuntimeStateNumber = Int
  type RuntimeTransitionNumber = Int


  object Channel {
    def create(ch: Seq[Any]): Channel = Channel(ch(0).asInstanceOf[String], ch(1).asInstanceOf[Double].toInt, ch(2).asInstanceOf[String], ch(3).asInstanceOf[String])

    def toTuple(x: Channel): (ProcessIdentifier, RuntimeProcessInstanceNumber, SubjectIdentifier, AgentIdentifier) = (x.processModelID, x.processInstanceNumber, x.subjectID, x.agent)
    def unapplySeq(x: Channel): Seq[Any] = Seq(x.processModelID, x.processInstanceNumber, x.subjectID, x.agent)

    private val tupledOrdering: Ordering[(ProcessIdentifier, RuntimeProcessInstanceNumber, SubjectIdentifier, AgentIdentifier)] = Ordering.Tuple4(Ordering.String, Ordering.Int, Ordering.String, Ordering.String).reverse

    val ordering: Ordering[Channel] = tupledOrdering.on { Channel.toTuple }
  }

  case class Channel(processModelID: ProcessIdentifier, processInstanceNumber: RuntimeProcessInstanceNumber, subjectID: SubjectIdentifier, agent: AgentIdentifier) extends Ordered[Channel] {
    override def compare(that: Channel): Int = Channel.ordering.compare(this, that)
    def toSeq: Seq[Any] = Channel.unapplySeq(this)
  }

  case class MacroInstance(ch: Channel, macroInstanceNumber: RuntimeMacroInstanceNumber, macroNumber: RuntimeMacroNumber, macroID: MacroIdentifier)

  case class ActiveState(mi: MacroInstance, stateNumber: RuntimeStateNumber, stateLabel: String, stateType: String, stateFunction: FunctionName) {
    val ch: Channel = mi.ch
    val MI: RuntimeMacroInstanceNumber = mi.macroInstanceNumber
  }

  case class Transition(sourceState: ActiveState, transitionNumber: RuntimeTransitionNumber, isHidden: Boolean, transitionLabel: String, targetStateLabel: String)

}
