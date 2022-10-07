package de.athalis.pass.processmodel.parser.ast.node.pass

import de.athalis.pass.processmodel.parser.ast.node.CustomNode
import de.athalis.pass.processmodel.parser.ast.node.NodeDebugger

import org.slf4j.LoggerFactory

import java.lang.System.{lineSeparator => EOL}

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
          case _: TransitionPropertyMsgTypeWithCorrelation => throw new IllegalArgumentException("TransitionPropertyMsgTypeWithCorrelation must be within TransitionPropertyToFromSubj or TransitionPropertyMsgFromSubj")
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

      "CommunicationTransitionNode" + EOL +
      "msgType: " + this.msgType + EOL +

      "subject: " + this.subject + EOL +
      "subjectCountMin: " + this.subjectCountMin + EOL +
      "subjectCountMax: " + this.subjectCountMax + EOL +
      "subjectVar: " + this.subjectVar + EOL +

      "content_var: " + this.content_var + EOL +
      "with_correlation_var: " + this.with_correlation_var + EOL +
      "new_correlation_var: " + this.new_correlation_var + EOL +
      "store_messages_var: " + this.store_messages_var + EOL +
      "store_receiver_var: " + this.store_receiver_var + EOL
    )
}
