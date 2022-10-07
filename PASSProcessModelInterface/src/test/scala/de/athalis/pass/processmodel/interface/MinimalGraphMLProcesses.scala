package de.athalis.pass.processmodel.interface

import de.athalis.pass.processmodel.tudarmstadt._

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.nio.file.Path

class MinimalGraphMLProcesses extends AnyFunSuite with Matchers {

  private val processesGraphMLDir = Path.of("processes", "graphml")

  private def allMacros(p: Process): Set[Macro] = {
    p.subjects.collect {
      case s: FullySpecifiedSubject => s.internalBehavior.additionalMacros + s.internalBehavior.mainMacro
    }.flatten ++ p.macros
  }

  private def checkMainMacro(m: Macro): Unit = {
    m.identifier shouldBe "Main"

    m.actions should have size 2 // one states plus END, that hides the default END state

    m.actions.map(_.state.function) shouldBe Set(Some(Terminate(None)), Some(AutoReceive))

    val receiveAction = m.actions.filter(_.state.function.contains(AutoReceive)).head
    receiveAction.outgoingTransitions should have size 1

    val t: Transition = receiveAction.outgoingTransitions.head

    t.attributes shouldBe Set(TransitionIsCancel)
  }

  private def checkDatenHolenMacro(m: Macro): Unit = {
    m.identifier shouldBe "Daten_holen"

    m.actions should have size 2 // one states plus END, that hides the default END state

    m.actions.map(_.state.function) shouldBe Set(Some(Return(Some("\"Daten\" from \"Datenmanger_historisch\""))), Some(AutoSend))
  }

  test("minimal_offen") {
    val file = processesGraphMLDir.resolve("minimal_offen.graphml")

    val processModels: Set[Process] = PASSProcessModelReaderInterface.readProcessModels(file).getProcessModels

    processModels should have size 1

    val p: Process = processModels.head

    p.subjects should have size 1

    val A: Subject = p.subjects.head
    A.identifier shouldBe "A"

    A shouldBe an [FullySpecifiedSubject]
    val AS = A.asInstanceOf[FullySpecifiedSubject]

    AS.internalBehavior.additionalMacros shouldBe Symbol("empty")

    checkMainMacro(AS.internalBehavior.mainMacro)
  }

  test("minimal_geschlossen") {
    val file = processesGraphMLDir.resolve("minimal_geschlossen.graphml")

    val processModels: Set[Process] = PASSProcessModelReaderInterface.readProcessModels(file).getProcessModels

    processModels should have size 1

    val p: Process = processModels.head

    p.subjects should have size 1

    val A = p.subjects.head
    A.identifier shouldBe "A"

    A shouldBe an [FullySpecifiedSubject]
    val AS = A.asInstanceOf[FullySpecifiedSubject]

    AS.internalBehavior.additionalMacros shouldBe Symbol("empty")

    checkMainMacro(AS.internalBehavior.mainMacro)
  }


  test("macro_offen") {
    val file = processesGraphMLDir.resolve("macro_offen.graphml")

    val processModels: Set[Process] = PASSProcessModelReaderInterface.readProcessModels(file).getProcessModels

    processModels should have size 1

    val p: Process = processModels.head

    p.subjects should have size 1

    val A: Subject = p.subjects.head
    A.identifier shouldBe "A"

    A shouldBe an [FullySpecifiedSubject]
    val AS = A.asInstanceOf[FullySpecifiedSubject]

    AS.internalBehavior.additionalMacros should have size 1

    AS.internalBehavior.additionalMacros.map(_.identifier) shouldBe Set("Daten_holen")

    checkMainMacro(AS.internalBehavior.mainMacro)

    val Daten_holen = AS.internalBehavior.additionalMacros.head
    checkDatenHolenMacro(Daten_holen)
  }

  test("macro_geschlossen") {
    val file = processesGraphMLDir.resolve("macro_geschlossen.graphml")

    val processModels: Set[Process] = PASSProcessModelReaderInterface.readProcessModels(file).getProcessModels

    processModels should have size 1

    val p: Process = processModels.head

    p.subjects should have size 1

    val A: Subject = p.subjects.head
    A.identifier shouldBe "A"

    A shouldBe an [FullySpecifiedSubject]
    val AS = A.asInstanceOf[FullySpecifiedSubject]

    AS.internalBehavior.additionalMacros should have size 1

    AS.internalBehavior.additionalMacros.map(_.identifier) shouldBe Set("Daten_holen")

    checkMainMacro(AS.internalBehavior.mainMacro)

    val Daten_holen = AS.internalBehavior.additionalMacros.head
    checkDatenHolenMacro(Daten_holen)
  }



  test("group_offen") {
    val file = processesGraphMLDir.resolve("group_offen.graphml")

    val processModels: Set[Process] = PASSProcessModelReaderInterface.readProcessModels(file).getProcessModels

    processModels should have size 1

    val p: Process = processModels.head

    p.subjects should have size 1

    val A: Subject = p.subjects.head
    A.identifier shouldBe "A"

    A shouldBe an [FullySpecifiedSubject]
    val AS = A.asInstanceOf[FullySpecifiedSubject]

    AS.internalBehavior.additionalMacros shouldBe Symbol("empty")

    checkMainMacro(AS.internalBehavior.mainMacro)
  }

  test("group_double_offen") {
    val file = processesGraphMLDir.resolve("group_double_offen.graphml")

    val processModels: Set[Process] = PASSProcessModelReaderInterface.readProcessModels(file).getProcessModels

    processModels should have size 1

    val p: Process = processModels.head

    p.subjects should have size 2
    p.subjects.map(_.identifier) shouldBe Set("A", "B")
  }


  test("process_offen") {
    val file = processesGraphMLDir.resolve("process_offen.graphml")

    val processModels: Set[Process] = PASSProcessModelReaderInterface.readProcessModels(file).getProcessModels

    processModels should have size 1

    val p: Process = processModels.head

    p.identifier shouldBe "XYZ"

    p.subjects.map(_.identifier) shouldBe Set("A")
    allMacros(p).map(_.identifier) shouldBe Set("Main")
  }

  test("process_geschlossen") {
    val file = processesGraphMLDir.resolve("process_geschlossen.graphml")

    val processModels: Set[Process] = PASSProcessModelReaderInterface.readProcessModels(file).getProcessModels

    processModels should have size 1

    val p: Process = processModels.head

    p.identifier shouldBe "XYZ"

    p.subjects.map(_.identifier) shouldBe Set("A")
    allMacros(p).map(_.identifier) shouldBe Set("Main")
  }


  test("process_macro_offen") {
    val file = processesGraphMLDir.resolve("process_macro_offen.graphml")

    val processModels: Set[Process] = PASSProcessModelReaderInterface.readProcessModels(file).getProcessModels

    processModels should have size 1

    val p: Process = processModels.head

    p.identifier shouldBe "XYZ"

    p.subjects.map(_.identifier) shouldBe Set("A")
    allMacros(p).map(_.identifier) shouldBe Set("Main", "Daten_holen")
  }

  test("process_macro_geschlossen") {
    val file = processesGraphMLDir.resolve("process_macro_geschlossen.graphml")

    val processModels: Set[Process] = PASSProcessModelReaderInterface.readProcessModels(file).getProcessModels

    processModels should have size 1

    val p: Process = processModels.head

    p.identifier shouldBe "XYZ"

    p.subjects.map(_.identifier) shouldBe Set("A")
    allMacros(p).map(_.identifier) shouldBe Set("Main", "Daten_holen")
  }

  test("process_group_double_offen") {
    val file = processesGraphMLDir.resolve("process_group_double_offen.graphml")

    val processModels: Set[Process] = PASSProcessModelReaderInterface.readProcessModels(file).getProcessModels

    processModels should have size 1

    val p: Process = processModels.head

    p.identifier shouldBe "XYZ"

    p.subjects should have size 2
    p.subjects.map(_.identifier) shouldBe Set("A", "B")
  }


  test("doppelt_offen") {
    val file = processesGraphMLDir.resolve("doppelt_offen.graphml")

    val processModels: Set[Process] = PASSProcessModelReaderInterface.readProcessModels(file).getProcessModels

    processModels should have size 2

    processModels.map(_.identifier) shouldBe Set("UVW", "XYZ")

    val p1: Process = processModels.find(_.identifier == "XYZ").get
    val p2: Process = processModels.find(_.identifier == "UVW").get

    p1.subjects.map(_.identifier) shouldBe Set("A", "B")
    allMacros(p1).map(_.identifier) shouldBe Set("Main", "M1", "M2")

    p2.subjects.map(_.identifier) shouldBe Set("A", "B")
    allMacros(p2).map(_.identifier) shouldBe Set("Main", "M1", "M2")
  }


  test("external") {
    val file = processesGraphMLDir.resolve("external.graphml")

    val processModels: Set[Process] = PASSProcessModelReaderInterface.readProcessModels(file).getProcessModels

    processModels should have size 1

    val p: Process = processModels.head

    p.identifier shouldBe "XYZ"

    p.subjects.map(_.identifier) shouldBe Set("A", "B")
    allMacros(p).map(_.identifier) shouldBe Set("Main")


    val A: Subject = p.subjects.find(_.identifier == "A").get
    val B: Subject = p.subjects.find(_.identifier == "B").get

    A shouldBe a [FullySpecifiedSubject]
    B shouldBe a [DefinedInterfaceSubject]

    val bS = B.asInstanceOf[DefinedInterfaceSubject]

    bS.process shouldBe "C"
    bS.subject shouldBe "UVW"
  }

  test("action_selectAgents") {
    val file = processesGraphMLDir.resolve("action_selectAgents.graphml")

    val processModels: Set[Process] = PASSProcessModelReaderInterface.readProcessModels(file).getProcessModels

    processModels should have size 1

    val p: Process = processModels.head

    p.subjects should have size 2

    val AO: Set[Subject] = p.subjects.filter(_.identifier == "A")

    AO should have size 1

    val A: Subject = AO.head
    A.identifier shouldBe "A"

    A shouldBe an [FullySpecifiedSubject]
    val AS = A.asInstanceOf[FullySpecifiedSubject]

    AS.internalBehavior.additionalMacros shouldBe Symbol("empty")


    val m: Macro = AS.internalBehavior.mainMacro

    m.actions should have size 2 // one states plus END, that hides the default END state

    m.actions.map(_.state.function) shouldBe Set(Some(Terminate(None)), Some(SelectAgents("b1", "B", 1, 2)))

    val selectAction: Action = m.actions.filter(_.state.function.exists(_.isInstanceOf[SelectAgents])).head
    selectAction.outgoingTransitions should have size 1

    val t: Transition = selectAction.outgoingTransitions.head

    t.attributes shouldBe Symbol("empty")

    t.condition shouldBe Symbol("empty")
  }
}
