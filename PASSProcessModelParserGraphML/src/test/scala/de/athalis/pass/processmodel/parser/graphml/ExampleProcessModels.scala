package de.athalis.pass.processmodel.parser.graphml

import de.athalis.pass.processmodel.parser.ast.node.pass.ProcessNode
import de.athalis.pass.processmodel.parser.graphml.parser.GraphMLParser
import de.athalis.pass.processmodel.parser.graphml.parser.GraphMLParser.MsgTypes

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.nio.file.Path

class ExampleProcessModels extends AnyFunSuite with Matchers {

  private val processesDir = Path.of("processes")

  test("ServiceDesk") {
    val file = processesDir.resolve("ServiceDesk.graphml")

    val processModels: Set[(ProcessNode, MsgTypes)] = GraphMLParser.loadProcessModels(file)

    processModels should have size 1

    val (p: ProcessNode, msgTypes: MsgTypes) = processModels.head

    p.getSubjects.map(_.id) shouldBe Set("Service Desk", "Employee", "Manager")
  }

  test("BusinessTrip") {
    val file = processesDir.resolve("BusinessTripSimple.graphml")

    val processModels = GraphMLParser.loadProcessModels(file)

    processModels should have size 1

    val (p: ProcessNode, msgTypes: MsgTypes) = processModels.head

    p.getSubjects.map(_.id) shouldBe Set("Mitarbeiter", "Vorgesetzter")
  }

  test("InquiryExample_v00c") {
    val file = processesDir.resolve("InquiryExample_v00c.graphml")

    val processModels: Set[(ProcessNode, MsgTypes)] = GraphMLParser.loadProcessModels(file)

    processModels should have size 1

    val (p: ProcessNode, msgTypes: MsgTypes) = processModels.head

    p.getSubjects.map(_.id) shouldBe Set("Principal", "Contractor")
  }

  test("Observer") {
    val file = processesDir.resolve("Observer.graphml")

    val processModels: Set[(ProcessNode, MsgTypes)] = GraphMLParser.loadProcessModels(file)

    processModels should have size 1

    val (p: ProcessNode, msgTypes: MsgTypes) = processModels.head

    p.getSubjects.map(_.id) shouldBe Set("Manager", "Worker")
  }

  test("VarManCorrelation_v00d") {
    val file = processesDir.resolve("VarManCorrelation_v00d.graphml")

    val processModels: Set[(ProcessNode, MsgTypes)] = GraphMLParser.loadProcessModels(file)

    processModels should have size 1

    val (p: ProcessNode, msgTypes: MsgTypes) = processModels.head

    p.getSubjects.map(_.id) shouldBe Set("Principal", "Contractor")
  }

  test("Angebotsanforderung") {
    val file = processesDir.resolve("Angebotsanforderung.graphml")

    val processModels: Set[(ProcessNode, MsgTypes)] = GraphMLParser.loadProcessModels(file)

    processModels should have size 1

    val (p: ProcessNode, msgTypes: MsgTypes) = processModels.head

    p.getSubjects.map(_.id) shouldBe Set("Principal", "Contractor")
    p.getAllMacros.map(_.id) shouldBe Set("Main", "RetrieveOffers")
  }

  test("Extern_und_IPvoll") {
    val file = processesDir.resolve("Extern_und_IPvoll.graphml")

    val processModels: Set[ProcessNode] = GraphMLParser.loadProcessModels(file).map(_._1)

    processModels should have size 2
    processModels.map(_.id) shouldBe Set("VSE", "[ui!]")



    val vse = processModels.find(_.id == "VSE").get

    vse.getSubjects should have size 2
    vse.getSubjects.map(_.id) shouldBe Set("Netzüberwacher", "HolonManager")

    val vseNetz = vse.getSubjects.find(_.id == "Netzüberwacher").get
    val vseManager = vse.getSubjects.find(_.id == "HolonManager").get

    vseNetz.isInterfaceSubject shouldBe true
    vseManager.isInterfaceSubject shouldBe false

    vseNetz.externalProcessID shouldBe "[ui!]"
    vseNetz.externalSubjectID shouldBe "Netzüberwacher"

    vseManager.getInputPoolSize shouldBe 10



    val ui = processModels.find(_.id == "[ui!]").get

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
