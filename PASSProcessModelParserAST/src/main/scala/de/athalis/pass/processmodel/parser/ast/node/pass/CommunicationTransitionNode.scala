package de.athalis.pass.processmodel.parser.ast.node.pass

import de.athalis.pass.processmodel.parser.ast.node.CustomNode
import de.athalis.pass.processmodel.parser.ast.node.NodeDebugger

import org.slf4j.LoggerFactory

object CommunicationTransitionNode {
  private val logger = LoggerFactory.getLogger(CommunicationTransitionNode.getClass)

  val PARSEROptional: java.util.function.Function[Seq[Option[CommunicationTransitionProperty]], CommunicationTransitionNode] = (from) => {
    PARSER(from.flatten)
  }

  val PARSER: java.util.function.Function[Seq[CommunicationTransitionProperty], CommunicationTransitionNode] = (from) => {
      logger.trace("CommunicationTransitionNode create")

      NodeDebugger.trace(from)

      val node = new CommunicationTransitionNode()

      for (propertyNode <- from) {
        logger.trace("handling " + propertyNode)

        propertyNode match {
          case TransitionPropertyMsgToSubj(msgTypeWithCorrelation, subject, inVar, subjectCount) => {
              node.msgType = msgTypeWithCorrelation.msgType
              node.with_correlation_var = msgTypeWithCorrelation.correlation.getOrElse("")
              node.subject = subject

              if (inVar.isDefined) node.subjectVar = inVar.get

              if (subjectCount.isDefined) {
                node.subjectCountMin = subjectCount.get.min
                node.subjectCountMax = subjectCount.get.max
              }
            }
          case TransitionPropertyMsgFromSubj(msgTypeWithCorrelation, subject, inVar, subjectCount) => {
              node.msgType = msgTypeWithCorrelation.msgType
              node.with_correlation_var = msgTypeWithCorrelation.correlation.getOrElse("")
              node.subject = subject

              if (inVar.isDefined) node.subjectVar = inVar.get

              if (subjectCount.isDefined) {
                node.subjectCountMin = subjectCount.get.min
                node.subjectCountMax = subjectCount.get.max
              }
            }
          case TransitionPropertyStoreMessagesIn(x) => {
            node.store_messages_var = x
          }
          case TransitionPropertyStoreReceiverIn(x) => {
            node.store_receiver_var = x
          }
          case TransitionPropertyWithContentOf(x) => {
              node.content_var = x
            }
          case TransitionPropertyWithNewCorrelation(x) => {
              node.new_correlation_var = x
            }
          case x: TransitionPropertyMsgTypeWithCorrelation => throw new IllegalArgumentException("TransitionPropertyMsgTypeWithCorrelation must be within TransitionPropertyToFromSubj or TransitionPropertyMsgFromSubj")
        }
      }

      node
  }
}

class CommunicationTransitionNode extends CustomNode {
  var msgType: String = ""

  var subject: String = ""
  var subjectCountMin: Int = 1
  var subjectCountMax: Int = 1 // 0 means *
  var subjectVar: String = ""

  var content_var: String = "" // either "with" or "store"
  var with_correlation_var: String = ""
  var new_correlation_var: String = ""
  var store_messages_var: String = ""
  var store_receiver_var: String = ""

  override def toString: String = (

      "CommunicationTransitionNode\n" +
      "msgType: " + this.msgType + "\n" +

      "subject: " + this.subject + "\n" +
      "subjectCountMin: " + this.subjectCountMin + "\n" +
      "subjectCountMax: " + this.subjectCountMax + "\n" +
      "subjectVar: " + this.subjectVar + "\n" +

      "content_var: " + this.content_var + "\n" +
      "with_correlation_var: " + this.with_correlation_var + "\n" +
      "new_correlation_var: " + this.new_correlation_var + "\n" +
      "store_messages_var: " + this.store_messages_var + "\n" +
      "store_receiver_var: " + this.store_receiver_var + "\n"
    )
}
