package de.athalis.pass.ui

import de.athalis.coreasm.base.Typedefs._
import de.athalis.coreasm.binding.akka.AkkaStorageBinding

import de.athalis.pass.processmodel.tudarmstadt.Types.AgentIdentifier
import de.athalis.pass.processmodel.tudarmstadt.Types.FunctionName
import de.athalis.pass.processmodel.tudarmstadt.Types.MacroIdentifier
import de.athalis.pass.processmodel.tudarmstadt.Types.ProcessIdentifier
import de.athalis.pass.processmodel.tudarmstadt.Types.SubjectIdentifier
import de.athalis.pass.semantic.Activities._
import de.athalis.pass.semantic.Semantic
import de.athalis.pass.semantic.Typedefs._
import de.athalis.pass.ui.loading._
import de.athalis.pass.ui.util.UIInputGetter

import de.athalis.util._
import de.athalis.util.jline._

import akka.event.LoggingAdapter
import akka.util.Timeout

import org.jline.builtins.Completers.FileNameCompleter
import org.jline.builtins.Completers.TreeCompleter
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Terminal
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle

import java.nio.file.Path

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object PASSInterpreterConsole {
  def selectAgents(subjectID: SubjectIdentifier, possibleAgents: Set[AgentIdentifier], min: Int, max: Int)(implicit terminal: Terminal): Set[String] = {
    var selectedAgents: Set[AgentIdentifier] = Set()
    var more = true

    println("Known Agents: " + possibleAgents.mkString(", "))

    while (more && (max == 0 || selectedAgents.size < max)) {
      val possibleAgentsList: Seq[AgentIdentifier] = (possibleAgents -- selectedAgents).toSeq.sorted

      val info = if (selectedAgents.size >= min) {" (leave empty to use only " + selectedAgents.size + " agents)"} else {""}

      val promptMessage = if (max > 0) {
        "Agent name (" + (selectedAgents.size+1) + "/" + max + ") for '" + subjectID + "'" + info + ": "
      }
      else {
        "Agent name (" + (selectedAgents.size+1) + ") for '" + subjectID + "'" + info + ": "
      }

      val l = JLineHelper.readLine(promptMessage, possibleAgentsList)

      val agentName: AgentIdentifier = l.trim

      if (agentName == "") {
        if (selectedAgents.size >= min) {
          more = false
        }
        else {
          println("The Agents name must not be empty unless the minimal amount of agents is selected!")
        }
      }
      else if (selectedAgents.contains(agentName)) {
        // TODO: this should be possible
        println("You've already chosen that Agent!")
      }
      else {
        selectedAgents += agentName
      }
    }

    selectedAgents
  }
}

class PASSInterpreterConsole()(implicit timeout: Timeout, logger: LoggingAdapter, executionContext: ExecutionContext, binding: AkkaStorageBinding, terminal: Terminal) {
  val processUI = new ProcessUI(binding)
  val loader = new ActivityLoader()
  val uiInputGetter: InputGetter = new UIInputGetter

  val activityReader: LineReader = initActivityReader()

  var availableActivities:          Seq[PASSActivity[_ <: PASSActivityInput]] = Seq()
  var availableActivitiesString:    Option[String] = None

  var runningProcessModelInstances: Seq[RuntimeProcessInstanceNumber] = Seq()
  var runningSubjects:              Set[Channel]                      = Set()
  var startAbleProcessModels:       Seq[ProcessIdentifier]            = Seq()

  var awaitASMStep: Option[Future[Any]] = None

  def printHelp(): Unit = {
    processUI.printHelp()

    val txt =
"""
  You can quit this shell with one of these commands: `q`, `quit` or `exit`.

  If you enter an empty line the available activities are reloaded.

  The activity '0' shows all running subjects with their IPs and with all active states, regardless of their execution state (i.e. states with a lower priority will be shown).

  You can display this information with the command `help`.
"""
    println(txt)
  }

  def initActivityReader(): LineReader = {
    val readerBuilder: LineReaderBuilder = LineReaderBuilder
      .builder()
      .terminal(terminal)

    val runningProcessModelInstancesC = new DynamicStringsCompleter(
        () => runningProcessModelInstances.map(_.toString)
      )

    val startAbleProcessModelsC = new DynamicStringsCompleter(
        () => startAbleProcessModels
      )

    val availableActivitiesC = new DynamicStringsCompleter(
        () => for (i <- availableActivities.indices) yield (i + 1).toString
      )


    val c2 = new TreeCompleter(
      TreeCompleter.node("reload"),
      TreeCompleter.node("help"),
      TreeCompleter.node("exit"),
      TreeCompleter.node("quit"),
      TreeCompleter.node("process",
        TreeCompleter.node("kill", TreeCompleter.node(runningProcessModelInstancesC)),
        TreeCompleter.node("start", TreeCompleter.node(startAbleProcessModelsC)),
        TreeCompleter.node("load", TreeCompleter.node(new FileNameCompleter())) // TODO: add completion for multiple files
      ),
      TreeCompleter.node(availableActivitiesC)
    )

    readerBuilder.completer(c2)


    val reader = readerBuilder.build()
    reader.setVariable(LineReader.HISTORY_FILE, Path.of(".console-history").toAbsolutePath)
    reader
  }

  private def toTaskTuple(a: PASSActivityTask[_ <: PASSActivityInput]): (PASSActivityTask[_ <: PASSActivityInput], String) = {
    (a, a.toActivityString)
  }

  private def toUIPathAndTextTuple(a: PASSActivityAgent[_ <: PASSActivityInput]): (PASSActivityAgent[_ <: PASSActivityInput], UIPath, String) = {
    (a, UIPath(a.state), a.toActivityString)
  }

  private def isJustMainMacro(states: Seq[ActiveState]): Boolean = (states.map(_.MI).toSet == Set(1))

  case class UIPath(processModelID: ProcessIdentifier, processInstance: RuntimeProcessInstanceNumber, subjectID: SubjectIdentifier, agent: AgentIdentifier, macroNumber: RuntimeMacroNumber, macroID: MacroIdentifier, macroInstanceNumber: RuntimeMacroInstanceNumber, stateNumber: RuntimeStateNumber, stateLabel: String, stateType: String, stateFunction: FunctionName)
  object UIPath {
    private val tupledOrdering: Ordering[(ProcessIdentifier, RuntimeProcessInstanceNumber, SubjectIdentifier, AgentIdentifier, RuntimeMacroNumber, RuntimeMacroInstanceNumber, RuntimeStateNumber)] = Ordering.Tuple7(Ordering.String, Ordering.Int, Ordering.String, Ordering.String, Ordering.Int, Ordering.Int, Ordering.Int).reverse

    val ordering: Ordering[UIPath] = tupledOrdering.on { uiPath => {
      (uiPath.processModelID, uiPath.processInstance, uiPath.subjectID, uiPath.agent, uiPath.macroNumber, uiPath.macroInstanceNumber, uiPath.stateNumber)
    }}

    def apply(state: ActiveState): UIPath = {
      UIPath(
        state.ch.processModelID,
        state.ch.processInstanceNumber,
        state.ch.subjectID,
        state.ch.agent,
        state.mi.macroNumber,
        state.mi.macroID,
        state.mi.macroInstanceNumber,
        state.stateNumber,
        state.stateLabel,
        state.stateType,
        state.stateFunction
      )
    }
  }

  def transformActivities(availableActivitiesSetF: Future[Set[PASSActivity[_ <: PASSActivityInput]]]): Future[(Seq[PASSActivity[_ <: PASSActivityInput]], Option[String])] = async {
    val availableActivities = await(availableActivitiesSetF).toSeq

    if (availableActivities.isEmpty) {
      (Seq(), None)
    }
    else {

      val agentActivities: Seq[PASSActivityAgent[_ <: PASSActivityInput]] = availableActivities.collect { case x: PASSActivityAgent[_] => x }
      val taskActivities: Seq[PASSActivityTask[_ <: PASSActivityInput]] = availableActivities.collect { case x: PASSActivityTask[_] => x }

      val availableAgentActivitiesWithUIPathAndText: Seq[(PASSActivityAgent[_ <: PASSActivityInput], UIPath, String)] = agentActivities.map(toUIPathAndTextTuple)
      val availableTaskActivitiesWithText: Seq[(PASSActivityTask[_ <: PASSActivityInput], String)] = taskActivities.map(toTaskTuple)

      var availableActivitiesSorted = Seq.empty[PASSActivity[_ <: PASSActivityInput]]
      val out = new StringBuilder
      var i = 1


      if (availableTaskActivitiesWithText.nonEmpty) {
        val availableTaskActivitiesWithStringSorted = availableTaskActivitiesWithText.sortBy(_._2)

        for (x <- availableTaskActivitiesWithStringSorted) {
          val activity = x._1
          val text = x._2

          availableActivitiesSorted :+= activity

          out.append(new AttributedString(s" $i) $text\n", AttributedStyle.BOLD.foreground(AttributedStyle.RED)).toAnsi)

          i += 1
        }
      }


      if (availableAgentActivitiesWithUIPathAndText.nonEmpty) {
        if (out.nonEmpty) {
          out.append("\n")
        }

        val agentActivitiesSorted = availableAgentActivitiesWithUIPathAndText.sortBy(_._3).sortBy(_._2)(UIPath.ordering.reverse)

        var lastUIPath: Option[UIPath] = None

        var hadProcessSegment = false
        var hadSubjectSegment = false

        for (x <- agentActivitiesSorted) {
          val activity = x._1
          val uiPath = x._2
          val text = x._3

          val showMacroIDs: Boolean = !isJustMainMacro(agentActivitiesSorted.map(_._1).filter(_.state.ch == activity.state.ch).map(_.state))

          var printNextSegment = lastUIPath.isEmpty

          if (printNextSegment || (lastUIPath.get.processInstance != uiPath.processInstance)) {
            if (hadProcessSegment) {
              out.append("\n\n")
            }
            out.append(s"Process '${uiPath.processModelID}' (Instance ${uiPath.processInstance})\n")
            printNextSegment = true
            hadProcessSegment = true
            hadSubjectSegment = false
          }

          if (printNextSegment || (lastUIPath.get.subjectID != uiPath.subjectID) || (lastUIPath.get.agent != uiPath.agent)) {
            if (hadSubjectSegment) {
              out.append("\n")
            }
            out.append(s" Subject '${uiPath.subjectID}' executed by Agent '${uiPath.agent}'\n")
            printNextSegment = true
            hadSubjectSegment = true
          }

          if (showMacroIDs && (printNextSegment || (lastUIPath.get.macroInstanceNumber != uiPath.macroInstanceNumber))) {
            out.append(s"  Macro '${uiPath.macroID}' (Instance ${uiPath.macroInstanceNumber})\n")
            printNextSegment = true
          }

          if (printNextSegment || (lastUIPath.get.stateNumber != uiPath.stateNumber)) {
            if (showMacroIDs) {
              out.append(" ")
            }

            val stateType = uiPath.stateType match {
              case "action" => uiPath.stateFunction
              case stateType => stateType
            }

            out.append(s"  State '${uiPath.stateLabel}' (${stateType})\n")
          }

          if (showMacroIDs) {
            out.append(" ")
          }

          availableActivitiesSorted :+= x._1

          out.append(new AttributedString(s"   $i) $text\n", AttributedStyle.BOLD.foreground(AttributedStyle.RED)).toAnsi)


          lastUIPath = Some(uiPath)
          i += 1
        }
      }


      (availableActivitiesSorted, Some(out.toString))
    }
  }

  def update(): Unit = {
    val start = System.nanoTime()

    awaitASMStep match {
      case None => ()
      case Some(wait) if wait.isCompleted => ()
      case Some(wait) => {
        wait.blockingWait()
        println("waited " + (System.nanoTime() - start) / 1e6 + "ms for the current ASM step to complete (otherwise we would load the exact same data)")
      }
    }

    val runningSubjectsF         = Semantic.runningSubjects.loadAndGetAsync()
    val runningProcessInstancesF = runningSubjectsF.map(x => x.map(_.processInstanceNumber).toSeq.sorted)
    val startAbleProcessModelsF  = Semantic.startAbleProcessModels.loadAndGetAsync().map(_.toSeq.sortBy(x => x))

    val availableActivitiesSetF  = loader.loadAllAvailableActivitiesAsync(runningSubjectsF)
    val activitiesF              = transformActivities(availableActivitiesSetF)

    val f = Future.sequence(Seq(runningSubjectsF, runningProcessInstancesF, startAbleProcessModelsF, activitiesF))

    // TODO: non-blocking? no global state?..
    Try(f.blockingWait()) match {
      case Success(_)  => {
        runningSubjects              = runningSubjectsF.blockingWait()
        runningProcessModelInstances = runningProcessInstancesF.blockingWait()
        startAbleProcessModels       = startAbleProcessModelsF.blockingWait()

        val activities = activitiesF.blockingWait()
        availableActivities       = activities._1
        availableActivitiesString = activities._2

        awaitASMStep = Some(binding.waitForASMStep())
      }
      case Failure(ex) => {
        logger.debug("failed to update available Activities: {}", ex.getNiceStackTraceString)
        logger.error("failed to update available Activities: {}", ex.getMessage)

        runningSubjects              = Set()
        runningProcessModelInstances = Seq()
        startAbleProcessModels       = Seq()
        availableActivities          = Seq()
        availableActivitiesString    = None

        awaitASMStep = None
      }
    }

    val end = System.nanoTime()

    Thread.`yield`() // give the logger a chance to print before we want to

    println("")
    println("updating took " + (end - start)/1e6 + "ms")
  }

  def run(): Unit = {
    printHelp()

    var running = true
    while (running) {
      update()

      if (runningSubjects.isEmpty) {
        println("")
        println("There are no process models being executed! You can start one with `process start PROCESSNAME`")
      }

      if (startAbleProcessModels.isEmpty) {
        println("")
        println("There are no process models which could be started! You can load one with `process load FILENAME`")
      }
      else {
        println("")
        println("Start-able process models: " + startAbleProcessModels.mkString(", "))
      }




      println("")
      println("Available Activities:")
      println("")

      if (availableActivitiesString.isEmpty) {
        println("No Activities available! Please wait for the process and reload the available activities again in a moment. The process may have terminated or is stuck. Maybe there is no process running?")
      }
      else {
        println(availableActivitiesString.get)
      }

      val input: String = JLineHelper.readLine("> ", activityReader)

      if (input == null) {
        running = false
      }
      else {
        val l = input.trim

        if (l == "" || l == "reload") {
          // skip...
        }
        else if (l == "process") {
          processUI.printHelp()
        }
        else if (l.startsWith("process ")) {
          Try(processUI.run(l.substring("process ".length))) match {
            case Success(_)  => ()
            case Failure(ex) => {
              logger.warning("failed to execute input '{}': {}", l, ex.getNiceStackTraceString)
              logger.error("failed to execute input '{}': {}", l, ex.getMessage)
            }
          }
        }
        else if (l == "help" || l == "h" || l == "info" || l == "version") {
          printHelp()
        }
        else if (l == "0") {
          printState()
        }
        else if (l == "exit" || l == "quit" || l == "q") {
          running = false
        }
        else {
          Try {
            val i = l.toInt - 1 // remove added 1
            val updates = availableActivities(i).getASMUpdates(uiInputGetter)
            binding.storeAsync(updates).blockingWait()
          } match {
            case Success(UpdateStored) => {
              awaitASMStep = Some(binding.waitForASMStep())
            }
            case Success(UpdateFailed) => {
              logger.error("failed to execute input '{}'", l)
              awaitASMStep = None
            }
            case Failure(ex) => {
              logger.debug("failed to execute input '{}': {}", l, ex.getNiceStackTraceString)
              logger.error("failed to execute input '{}': {}", l, ex.getMessage)
              awaitASMStep = None
            }
          }
        }
      }
    }
  }

  def printState(): Unit = {
    val out: String = Try(Debug.getGlobalState().blockingWait()).getOrElse("Error loading state")

    println(out)
  }
}
