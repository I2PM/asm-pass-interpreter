package de.athalis.pass.semantic

import de.athalis.coreasm.base.Typedefs._

import de.athalis.pass.processmodel.tudarmstadt.Types.AgentIdentifier
import de.athalis.pass.processmodel.tudarmstadt.Types.ProcessIdentifier
import de.athalis.pass.processmodel.tudarmstadt.Types.SubjectIdentifier
import de.athalis.pass.semantic.Typedefs.Channel
import de.athalis.pass.semantic.Typedefs.RuntimeProcessInstanceNumber

object Helper {

  type TaskMap = Map[String, Any]

  def addTask(task: TaskMap): ASMUpdate = {
    AddToSet(Semantic.taskSetIn.location, task)
  }

  def removeTask(task: TaskMap): ASMUpdate = {
    RemoveFromSet(Semantic.taskSetOut.location, task)
  }

  def initializeAndStartSubjectTask(processModelID: ProcessIdentifier, processInstanceNumber: RuntimeProcessInstanceNumber, subjectID: SubjectIdentifier, agent: AgentIdentifier): TaskMap = {
    Map(
      "task" -> "InitializeAndStartSubject",
      "ch" -> Channel(processModelID, processInstanceNumber, subjectID, agent).toSeq
    )
  }

}
