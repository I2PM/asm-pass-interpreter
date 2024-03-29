package de.athalis.pass.processmodel.parser.ast.node

trait CustomNode {
  private var parent: Option[CustomNode] = None

  def setParent(parent: CustomNode): Unit = { this.parent = Some(parent) }

  def getParent: CustomNode = this.parent.get
}

trait MapAbleNode[+T] {
  def value: T
}
