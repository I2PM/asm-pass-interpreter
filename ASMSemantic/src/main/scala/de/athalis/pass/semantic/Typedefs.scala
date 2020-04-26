package de.athalis.pass.semantic

object Typedefs {

  object Channel {
    def create(ch: Seq[Any]): Channel = Channel(ch(0).asInstanceOf[String], ch(1).asInstanceOf[Double].toInt, ch(2).asInstanceOf[String], ch(3).asInstanceOf[String])

    def unapplySeq(x: Channel): Seq[Any] = Seq(x.processID, x.processInstanceNumber, x.subjectID, x.agent)

    private val tupledOrdering: Ordering[(String, Int, String, String)] = Ordering.Tuple4(Ordering.String, Ordering.Int, Ordering.String, Ordering.String).reverse

    val ordering: Ordering[Channel] = tupledOrdering.on { Channel.unapply(_).get }
  }

  case class Channel(processID: String, processInstanceNumber: Int, subjectID: String, agent: String) extends Ordered[Channel] {
    override def compare(that: Channel): Int = Channel.ordering.compare(this, that)
    def toSeq: Seq[Any] = Channel.unapplySeq(this)
  }

  case class MacroInstance(ch: Channel, macroInstanceNumber: Int, macroNumber: Int, macroID: String)

  case class ActiveState(mi: MacroInstance, stateNumber: Int, stateLabel: String, stateType: String, stateFunction: String) {
    val ch: Channel = mi.ch
    val MI: Int = mi.macroInstanceNumber
  }

  case class Transition(sourceState: ActiveState, transitionNumber: Int, isHidden: Boolean, transitionID: String, targetStateLabel: String)

}
