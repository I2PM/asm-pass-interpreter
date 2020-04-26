package de.athalis.coreasm.plugins.storage

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.coreasm.engine.interpreter._

object AkkaStorageRuleNode {
  val RULE_NAME: String = "AggregateAkkaStorage"
  private val logger: Logger = LoggerFactory.getLogger(AkkaStorageRuleNode.getClass)
}

class AkkaStorageRuleNode extends ASTNode(AkkaStoragePlugin.PLUGIN_NAME, ASTNode.RULE_CLASS, AkkaStorageRuleNode.RULE_NAME, null, new ScannerInfo()) {
  def this(node: AkkaStorageRuleNode) = this()
}
