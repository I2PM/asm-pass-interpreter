package de.athalis.pass.ui.util

import de.athalis.coreasm.binding.Binding

import de.athalis.pass.processmodel.tudarmstadt.Types.AgentIdentifier
import de.athalis.pass.processmodel.tudarmstadt.Types.SubjectIdentifier
import de.athalis.pass.semantic.Activities._
import de.athalis.pass.semantic.Semantic
import de.athalis.pass.semantic.Typedefs._
import de.athalis.pass.ui.PASSInterpreterConsole

import de.athalis.util._
import de.athalis.util.jline.JLineHelper

import akka.util.Timeout

import org.jline.terminal.Terminal

import java.lang.System.{lineSeparator => EOL}

import scala.concurrent.ExecutionContext
import scala.util.Try

class UIInputGetter(implicit val timeout: Timeout, executionContext: ExecutionContext, binding: Binding, terminal: Terminal) extends InputGetter {
  override def selectAgents(subjectID: SubjectIdentifier, min: Int, max: Int): PASSActivityInputAgents = {
    val knownAgents: Set[AgentIdentifier] = Semantic.aALL.loadAndGetAsync().blockingWait()
    val selectedAgents: Set[AgentIdentifier] = PASSInterpreterConsole.selectAgents(subjectID, knownAgents, min, max)
    PASSActivityInputAgents(selectedAgents)
  }

  override def setMessageContent(messageType: String, receivers: Set[Channel]): PASSActivityInputMessageContent = {
    val messageContent: String = JLineHelper.readLine("Receivers: " + receivers.mkString("{", ", ", "}") + EOL + "Message Content (\"" + messageType + "\"): ")

    PASSActivityInputMessageContent(messageContent)
  }

  override def performSelection(options: Seq[String], min: Int, max: Int): PASSActivityInputSelection = {
    if (min < 1) throw new IllegalArgumentException(f"`min` must be at least ${1}%d")

    for ((x, i) <- options.zipWithIndex) {
      println(f"[${(i+1)}%d] $x%s") // +1 as CoreASM is off-by-one relative to usual programming related list-indices, and it is also better to read
    }

    var selection: Set[Int] = Set()

    var abort: Boolean = false

    val isInvalidSelection = () => (selection.size < min || (max > 0 && selection.size > max))

    while (isInvalidSelection() && !abort) {
      val prompt = {
        val sep = "(separate multiple options with a simple space)"

        if (max == 0 && min == 1) f"Select at least one element $sep: "
        else if (max == 0) f"Select at least $min%,d elements $sep: "
        else if (max == 1) f"Select one element: "
        else if (max == min) f"Select $min%,d elements $sep%s: "
        else f"Select between $min%,d and $max%,d elements $sep%s: "
      }
      val input: String = JLineHelper.readLine(prompt)

      selection = input.split(" ").map(x => Try(x.trim.toInt).toOption).flatMap {
        case Some(x) if (x >= 1 && x <= options.size) => Some(x)
        case _ => None
      }.toSet

      if (isInvalidSelection()) {
        if (input == "quit") {
          abort = true
        }
        else {
          println("invalid selection, please try again. Type `quit` to abort.")
        }
      }
    }

    if (abort) {
      PASSActivityInputSelection(None)
    }
    else {
      PASSActivityInputSelection(Some(selection))
    }
  }
}
