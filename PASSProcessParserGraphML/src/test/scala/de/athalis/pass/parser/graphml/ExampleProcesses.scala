package de.athalis.pass.parser.graphml

import java.io.File

import de.athalis.pass.parser.graphml.parser.GraphMLParser
import de.athalis.pass.parser.graphml.parser.GraphMLParser.MsgTypes
import de.athalis.pass.parser.ast.pass.ProcessNode

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ExampleProcesses extends AnyFunSuite with Matchers {
  test("ServiceDesk") {
    val file = new File("processes/ServiceDesk.graphml")

    val processes: Set[(ProcessNode, MsgTypes)] = GraphMLParser.loadProcesses(file)

    processes should have size 1

    val (p: ProcessNode, msgTypes: MsgTypes) = processes.head

    p.getSubjects.map(_.id) shouldBe Set("Service Desk", "Employee", "Manager")
  }

  test("BusinessTrip") {
    val file = new File("processes/BusinessTripSimple.graphml")

    val processes = GraphMLParser.loadProcesses(file)

    processes should have size 1

    val (p: ProcessNode, msgTypes: MsgTypes) = processes.head

    p.getSubjects.map(_.id) shouldBe Set("Mitarbeiter", "Vorgesetzter")
  }

  test("InquiryExample_v00c") {
    val file = new File("processes/InquiryExample_v00c.graphml")

    val processes: Set[(ProcessNode, MsgTypes)] = GraphMLParser.loadProcesses(file)

    processes should have size 1

    val (p: ProcessNode, msgTypes: MsgTypes) = processes.head

    p.getSubjects.map(_.id) shouldBe Set("Principal", "Contractor")
  }

  test("Observer") {
    val file = new File("processes/Observer.graphml")

    val processes: Set[(ProcessNode, MsgTypes)] = GraphMLParser.loadProcesses(file)

    processes should have size 1

    val (p: ProcessNode, msgTypes: MsgTypes) = processes.head

    p.getSubjects.map(_.id) shouldBe Set("Manager", "Worker")
  }

  test("VarManCorrelation_v00d") {
    val file = new File("processes/VarManCorrelation_v00d.graphml")

    val processes: Set[(ProcessNode, MsgTypes)] = GraphMLParser.loadProcesses(file)

    processes should have size 1

    val (p: ProcessNode, msgTypes: MsgTypes) = processes.head

    p.getSubjects.map(_.id) shouldBe Set("Principal", "Contractor")
  }

  test("Angebotsanforderung") {
    val file = new File("processes/Angebotsanforderung.graphml")

    val processes: Set[(ProcessNode, MsgTypes)] = GraphMLParser.loadProcesses(file)

    processes should have size 1

    val (p: ProcessNode, msgTypes: MsgTypes) = processes.head

    p.getSubjects.map(_.id) shouldBe Set("Principal", "Contractor")
    p.getAllMacros.map(_.id) shouldBe Set("Main", "RetrieveOffers")
  }

  test("Extern_und_IPvoll") {
    val file = new File("processes/Extern_und_IPvoll.graphml")

    val processes: Set[ProcessNode] = GraphMLParser.loadProcesses(file).map(_._1)

    processes should have size 2
    processes.map(_.id) shouldBe Set("VSE", "[ui!]")



    val vse = processes.find(_.id == "VSE").get

    vse.getSubjects should have size 2
    vse.getSubjects.map(_.id) shouldBe Set("Netzüberwacher", "HolonManager")

    val vseNetz = vse.getSubjects.find(_.id == "Netzüberwacher").get
    val vseManager = vse.getSubjects.find(_.id == "HolonManager").get

    vseNetz.isInterfaceSubject shouldBe true
    vseManager.isInterfaceSubject shouldBe false

    vseNetz.externalProcessID shouldBe "[ui!]"
    vseNetz.externalSubjectID shouldBe "Netzüberwacher"

    vseManager.getInputPoolSize shouldBe 10



    val ui = processes.find(_.id == "[ui!]").get

    ui.getSubjects should have size 2
    ui.getSubjects.map(_.id) shouldBe Set("Netzüberwacher", "Holonmanager")

    val uiNetz = ui.getSubjects.find(_.id == "Netzüberwacher").get
    val uiManager = ui.getSubjects.find(_.id == "Holonmanager").get

    uiNetz.isInterfaceSubject shouldBe false
    uiManager.isInterfaceSubject shouldBe true

    uiNetz.getInputPoolSize shouldBe 1

    uiManager.externalProcessID shouldBe "VSE"
    uiManager.externalSubjectID shouldBe "HolonManager"
  }
}
