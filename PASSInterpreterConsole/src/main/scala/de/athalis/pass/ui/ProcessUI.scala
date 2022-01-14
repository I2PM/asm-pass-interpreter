package de.athalis.pass.ui

import de.athalis.coreasm.base.Typedefs._
import de.athalis.coreasm.binding.Binding

import de.athalis.pass.processmodel.PASSProcessModelCollection
import de.athalis.pass.processmodel.interface.PASSProcessModelReaderInterface
import de.athalis.pass.processmodel.tudarmstadt.Examples
import de.athalis.pass.processmodel.tudarmstadt.Process
import de.athalis.pass.processmodel.tudarmstadt.Types.ProcessIdentifier
import de.athalis.pass.processmodel.writer.asm.ProcessModelASMMapWrapper
import de.athalis.pass.processmodel.writer.asm.TUDarmstadtModel2ASMMap
import de.athalis.pass.processmodel.writer.asm.TUDarmstadtModel2ASMMap.ProcessModelsASMMap
import de.athalis.pass.semantic.Helper._
import de.athalis.pass.semantic.Typedefs.RuntimeProcessInstanceNumber

import de.athalis.util._

import akka.event.LoggingAdapter
import akka.util.Timeout

import java.io.File

class ProcessUI(binding: Binding)(implicit timeout: Timeout, logger: LoggingAdapter) {

  def printHelp(): Unit = {
    val txt =
s"""
  process load FILENAME
  process load FILENAME1${File.pathSeparatorChar}FILENAME2
  process start PROCESSMODELID
  process kill PROCESSINSTANCE
"""
    println(txt)
  }

  def run(l: String): Unit = {
    if (l.startsWith("load example")) {
      loadExampleProcessModels()
    }
    else if (l.startsWith("load ")) {
      loadProcessModels(l.substring("load ".length).trim)
    }
    else if (l.startsWith("start ")) {
      startProcessModel(l.substring("start ".length).trim) // TODO: works in REPL without trim?..
    }
    else if (l.startsWith("kill ")) {
      val pi: Int = l.substring("kill ".length).trim.toInt
      stop(pi)
    }
    else {
      printHelp()
    }
  }

  def loadExampleProcessModels(): UpdateResult = {
    val processModels: PASSProcessModelCollection[Process] = PASSProcessModelCollection(Examples.hotelBookingProcess, Examples.travelRequestProcess)

    logger.debug("example Process Models: {}", processModels)

    val processModelsConverted: PASSProcessModelCollection[ProcessModelASMMapWrapper] = TUDarmstadtModel2ASMMap.convert(processModels)

    val processModelsMap: ProcessModelsASMMap = ProcessModelASMMapWrapper.toProcessModelsASMMap(processModelsConverted)

    var updates: Seq[ASMUpdate] = Nil

    for ((processModelID, processModelMap) <- processModelsMap) {
      updates +:= addTask(Map(
        "task" -> "AddProcessModel",
        "processModelID"  -> processModelID,
        "processModelMap" -> processModelMap
      ))
    }

    binding.storeAsync(updates).blockingWait()
  }

  def loadProcessModels(paths: String): UpdateResult = {
    val processModels: PASSProcessModelCollection[Process] = PASSProcessModelReaderInterface.readProcessModels(paths)

    logger.debug("parsed Process Models: {}", processModels)

    val processModelsConverted: PASSProcessModelCollection[ProcessModelASMMapWrapper] = TUDarmstadtModel2ASMMap.convert(processModels)

    val processModelsMap: ProcessModelsASMMap = ProcessModelASMMapWrapper.toProcessModelsASMMap(processModelsConverted)

    var updates: Seq[ASMUpdate] = Nil

    for ((processModelID, processModelMap) <- processModelsMap) {
      updates +:= addTask(Map(
          "task" -> "AddProcessModel",
          "processModelID"  -> processModelID,
          "processModelMap" -> processModelMap
        ))
    }

    binding.storeAsync(updates).blockingWait()
  }

  def startProcessModel(processModelID: ProcessIdentifier): UpdateResult = {
    var updates: Seq[ASMUpdate] = Nil

    updates +:= addTask(Map(
        "task" -> "StartProcessModel",
        "processModelID" -> processModelID
      ))

    binding.storeAsync(updates).blockingWait()
  }

  def stop(pi: RuntimeProcessInstanceNumber): UpdateResult = {
    var updates: Seq[ASMUpdate] = Nil

    updates +:= addTask(Map(
        "task" -> "StopProcess",
        "PI" -> pi
      ))

    binding.storeAsync(updates).blockingWait()
  }
}
