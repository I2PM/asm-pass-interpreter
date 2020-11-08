package de.athalis.pass.processutil

import java.io.File

import de.athalis.pass.model.TUDarmstadtModel._

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class MinimalGraphMLProcesses extends AnyFunSuite with Matchers {

  private def allMacros(p: Process): Set[Macro] = {
    p.subjects.collect {
      case s: InternalSubject => s.internalBehavior.additionalMacros + s.internalBehavior.mainMacro
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

    m.actions.map(_.state.function) shouldBe Set(Some(Terminate(Some("\"Daten\" from \"Datenmanger_historisch\""))), Some(AutoSend))
  }

  test("minimal_offen") {
    val file = new File("processes/graphml/minimal_offen.graphml")

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(file)

    processes should have size 1

    val p: Process = processes.head

    p.subjects should have size 1

    val A: Subject = p.subjects.head
    A.identifier shouldBe "A"
    
    A shouldBe an [InternalSubject]
    val AS = A.asInstanceOf[InternalSubject]

    AS.internalBehavior.additionalMacros shouldBe 'empty

    checkMainMacro(AS.internalBehavior.mainMacro)
  }

  test("minimal_geschlossen") {
    val file = new File("processes/graphml/minimal_geschlossen.graphml")

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(file)

    processes should have size 1

    val p: Process = processes.head

    p.subjects should have size 1

    val A = p.subjects.head
    A.identifier shouldBe "A"
    
    A shouldBe an [InternalSubject]
    val AS = A.asInstanceOf[InternalSubject]

    AS.internalBehavior.additionalMacros shouldBe 'empty

    checkMainMacro(AS.internalBehavior.mainMacro)
  }


  test("macro_offen") {
    val file = new File("processes/graphml/macro_offen.graphml")

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(file)

    processes should have size 1

    val p: Process = processes.head

    p.subjects should have size 1

    val A: Subject = p.subjects.head
    A.identifier shouldBe "A"

    A shouldBe an [InternalSubject]
    val AS = A.asInstanceOf[InternalSubject]

    AS.internalBehavior.additionalMacros should have size 1

    AS.internalBehavior.additionalMacros.map(_.identifier) shouldBe Set("Daten_holen")

    checkMainMacro(AS.internalBehavior.mainMacro)

    val Daten_holen = AS.internalBehavior.additionalMacros.head
    checkDatenHolenMacro(Daten_holen)
  }

  test("macro_geschlossen") {
    val file = new File("processes/graphml/macro_geschlossen.graphml")

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(file)

    processes should have size 1

    val p: Process = processes.head

    p.subjects should have size 1

    val A: Subject = p.subjects.head
    A.identifier shouldBe "A"

    A shouldBe an [InternalSubject]
    val AS = A.asInstanceOf[InternalSubject]

    AS.internalBehavior.additionalMacros should have size 1

    AS.internalBehavior.additionalMacros.map(_.identifier) shouldBe Set("Daten_holen")

    checkMainMacro(AS.internalBehavior.mainMacro)

    val Daten_holen = AS.internalBehavior.additionalMacros.head
    checkDatenHolenMacro(Daten_holen)
  }



  test("group_offen") {
    val file = new File("processes/graphml/group_offen.graphml")

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(file)

    processes should have size 1

    val p: Process = processes.head

    p.subjects should have size 1

    val A: Subject = p.subjects.head
    A.identifier shouldBe "A"

    A shouldBe an [InternalSubject]
    val AS = A.asInstanceOf[InternalSubject]

    AS.internalBehavior.additionalMacros shouldBe 'empty

    checkMainMacro(AS.internalBehavior.mainMacro)
  }

  test("group_double_offen") {
    val file = new File("processes/graphml/group_double_offen.graphml")

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(file)

    processes should have size 1

    val p: Process = processes.head

    p.subjects should have size 2
    p.subjects.map(_.identifier) shouldBe Set("A", "B")
  }


  test("process_offen") {
    val file = new File("processes/graphml/process_offen.graphml")

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(file)

    processes should have size 1

    val p: Process = processes.head

    p.identifier shouldBe "XYZ"

    p.subjects.map(_.identifier) shouldBe Set("A")
    allMacros(p).map(_.identifier) shouldBe Set("Main")
  }

  test("process_geschlossen") {
    val file = new File("processes/graphml/process_geschlossen.graphml")

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(file)

    processes should have size 1

    val p: Process = processes.head

    p.identifier shouldBe "XYZ"

    p.subjects.map(_.identifier) shouldBe Set("A")
    allMacros(p).map(_.identifier) shouldBe Set("Main")
  }


  test("process_macro_offen") {
    val file = new File("processes/graphml/process_macro_offen.graphml")

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(file)

    processes should have size 1

    val p: Process = processes.head

    p.identifier shouldBe "XYZ"

    p.subjects.map(_.identifier) shouldBe Set("A")
    allMacros(p).map(_.identifier) shouldBe Set("Main", "Daten_holen")
  }

  test("process_macro_geschlossen") {
    val file = new File("processes/graphml/process_macro_geschlossen.graphml")

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(file)

    processes should have size 1

    val p: Process = processes.head

    p.identifier shouldBe "XYZ"

    p.subjects.map(_.identifier) shouldBe Set("A")
    allMacros(p).map(_.identifier) shouldBe Set("Main", "Daten_holen")
  }

  test("process_group_double_offen") {
    val file = new File("processes/graphml/process_group_double_offen.graphml")

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(file)

    processes should have size 1

    val p: Process = processes.head

    p.identifier shouldBe "XYZ"

    p.subjects should have size 2
    p.subjects.map(_.identifier) shouldBe Set("A", "B")
  }


  test("doppelt_offen") {
    val file = new File("processes/graphml/doppelt_offen.graphml")

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(file)

    processes should have size 2

    processes.map(_.identifier) shouldBe Set("UVW", "XYZ")

    val p1: Process = processes.find(_.identifier == "XYZ").get
    val p2: Process = processes.find(_.identifier == "UVW").get

    p1.subjects.map(_.identifier) shouldBe Set("A", "B")
    allMacros(p1).map(_.identifier) shouldBe Set("Main", "M1", "M2")

    p2.subjects.map(_.identifier) shouldBe Set("A", "B")
    allMacros(p2).map(_.identifier) shouldBe Set("Main", "M1", "M2")
  }


  test("external") {
    val file = new File("processes/graphml/external.graphml")

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(file)

    processes should have size 1

    val p: Process = processes.head

    p.identifier shouldBe "XYZ"

    p.subjects.map(_.identifier) shouldBe Set("A", "B")
    allMacros(p).map(_.identifier) shouldBe Set("Main")


    val A: Subject = p.subjects.find(_.identifier == "A").get
    val B: Subject = p.subjects.find(_.identifier == "B").get

    A shouldBe a [InternalSubject]
    B shouldBe a [DefinedInterfaceSubject]

    val bS = B.asInstanceOf[DefinedInterfaceSubject]

    bS.process shouldBe "C"
    bS.subject shouldBe "UVW"
  }

  test("action_selectAgents") {
    val file = new File("processes/graphml/action_selectAgents.graphml")

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(file)

    processes should have size 1

    val p: Process = processes.head

    p.subjects should have size 2

    val AO: Set[Subject] = p.subjects.filter(_.identifier == "A")

    AO should have size 1

    val A: Subject = AO.head
    A.identifier shouldBe "A"

    A shouldBe an [InternalSubject]
    val AS = A.asInstanceOf[InternalSubject]

    AS.internalBehavior.additionalMacros shouldBe 'empty


    val m: Macro = AS.internalBehavior.mainMacro

    m.actions should have size 2 // one states plus END, that hides the default END state

    m.actions.map(_.state.function) shouldBe Set(Some(Terminate(None)), Some(SelectAgents("b1", "B", 1, 2)))

    val selectAction: Action = m.actions.filter(_.state.function.exists(_.isInstanceOf[SelectAgents])).head
    selectAction.outgoingTransitions should have size 1

    val t: Transition = selectAction.outgoingTransitions.head

    t.attributes shouldBe 'empty

    t.condition shouldBe 'empty
  }
}
