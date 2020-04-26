package de.athalis.pass.ui

import java.io.File

import akka.event.LoggingAdapter
import akka.util.Timeout

import de.athalis.pass.model.TUDarmstadtModel.{Examples, Process}
import de.athalis.pass.semantic.Helper._
import de.athalis.coreasm.base.Typedefs._
import de.athalis.coreasm.binding.Binding
import de.athalis.pass.processutil.PASSProcessReaderUtil
import de.athalis.pass.writer.asm.Model2ASMMap
import de.athalis.pass.writer.asm.Model2ASMMap.ProcessMap
import de.athalis.util._

class ProcessUI(binding: Binding)(implicit timeout: Timeout, logger: LoggingAdapter) {

  def printHelp(): Unit = {
    val txt =
"""
  process load FILENAME
  process start PROCESSNAME
  process kill PROCESSINSTANCE
"""
    println(txt)
  }

  def run(l: String)(): Unit = {
    if (l.startsWith("load example")) {
      loadExample()
    }
    else if (l.startsWith("load ")) {
      load(l.substring("load ".length).trim)
    }
    else if (l.startsWith("start ")) {
      start(l.substring("start ".length).trim) // TODO: works in REPL without trim?..
    }
    else if (l.startsWith("kill ")) {
      val pi: Int = l.substring("kill ".length).trim.toInt
      stop(pi)
    }
    else {
      printHelp()
    }
  }

  def loadExample(): UpdateResult = {
    val processes: Set[Process] = Set(Examples.hotelBookingProcess, Examples.travelRequestProcess)

    logger.debug("parsed Processes: {}", processes)

    val processesMap: ProcessMap = Model2ASMMap.toMap(processes)

    var updates: Seq[ASMUpdate] = Nil

    for ((processID, processMap) <- processesMap) {
      updates +:= addTask(Map(
        "task" -> "AddProcess",
        "processID" -> processID,
        "processMap" -> processMap
      ))
    }

    binding.storeAsync(updates).blockingWait
  }

  def load(path: String): UpdateResult = {
    val file = new File(path)

    if (!file.isFile) {
      throw new IllegalArgumentException("not a file: " + file)
    }

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(file)

    logger.debug("parsed Processes: {}", processes)

    val processesMap: Map[String, Map[String, Any]] = processes.map(p => {
      (p.identifier -> Model2ASMMap.toMap(p))
    }).toMap

    var updates: Seq[ASMUpdate] = Nil

    for ((processID, processMap) <- processesMap) {
      updates +:= addTask(Map(
          "task" -> "AddProcess",
          "processID" -> processID,
          "processMap" -> processMap
        ))
    }

    binding.storeAsync(updates).blockingWait
  }

  def start(processID: String): UpdateResult = {
    var updates: Seq[ASMUpdate] = Nil

    updates +:= addTask(Map(
        "task" -> "StartProcess",
        "processID" -> processID
      ))

    binding.storeAsync(updates).blockingWait
  }

  def stop(pi: Int): UpdateResult = {
    var updates: Seq[ASMUpdate] = Nil

    updates +:= addTask(Map(
        "task" -> "StopProcess",
        "PI" -> pi
      ))

    binding.storeAsync(updates).blockingWait
  }
}
