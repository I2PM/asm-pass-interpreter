package de.athalis.pass.semantic

import de.athalis.coreasm.base.Typedefs._

object Helper {
  def addTask(task: Map[String, Any]): ASMUpdate = {
    AddToSet(Seq("taskSetIn"), task)
  }

  def removeTask(task: Map[String, Any]): ASMUpdate = {
    RemoveFromSet(Seq("taskSetOut"), task)
  }
}
