package de.athalis.pass.parser.graphml

import scala.xml.{Node => XMLNode}
import scala.collection.immutable._
import org.scalatest.FunSuite
import org.scalatest.Matchers
import org.scalatest.OptionValues._
import de.athalis.pass.parser.graphml.parser.GraphMLParser
import de.athalis.pass.parser.graphml.parser.GraphMLParser.MsgTypes
import de.athalis.pass.parser.graphml.structure.RGBColor

class EdgeNodeSpec extends FunSuite with Matchers {

  val keys = Util.keys

  val stateNodes = StateNodeSpec.nodes
  val passStateNodes = StateNodeSpec.nodeStates

  def createXMLEdge(id: String, source: String, target: String, labelText: String, lineColor: RGBColor = RGBColor.black, lineType: String = "line"): XMLNode = {
      <edge id={id} source={source} target={target}>
        <data key="d10"/>
        <data key="d12">
          <y:GenericEdge configuration="com.yworks.bpmn.Connection">
            <y:Path sx="37.83333333333334" sy="27.5" tx="0.0" ty="-15.0">
              <y:Point x="-2784.0541151041634" y="-581.1830624999997"/>
              <y:Point x="-2705.902096874997" y="-581.1830624999997"/>
            </y:Path>
            <y:LineStyle color={lineColor.mkString} type={lineType} width="1.0"/>
            <y:Arrows source="none" target="delta"/>
            <y:EdgeLabel alignment="left" backgroundColor="#FFFFFF" configuration="AutoFlippingLabel" distance="2.0" fontFamily="Dialog" fontSize="12" fontStyle="plain" height="17.96875" lineColor="#000000" modelName="centered" modelPosition="center" preferredPlacement="anywhere" ratio="0.5" textColor="#000000" visible="true" width="54.12109375" x="12.0155463541696" y="11.015606933594086">{labelText}<y:PreferredPlacementDescriptor angle="0.0" angleOffsetOnRightSide="0" angleReference="absolute" angleRotationOnRightSide="co" distance="-1.0" frozen="true" placement="anywhere" side="anywhere" sideReference="relative_to_edge_flow"/></y:EdgeLabel>
            <y:StyleProperties>
              <y:Property class="com.yworks.yfiles.bpmn.view.BPMNTypeEnum" name="com.yworks.bpmn.type" value="CONNECTION_TYPE_SEQUENCE_FLOW"/>
            </y:StyleProperties>
          </y:GenericEdge>
        </data>
      </edge>
  }


  test("minimal action edge") {
    val nodeXML = createXMLEdge("graphml-edge-x", "graphml-state-01", "graphml-state-01", "repeat")

    val edgeNode = Helper.parseEdge(nodeXML, keys, stateNodes)

    val s = "FIXME"
    val (_, transition) = GraphMLParser.parseTransition(s, edgeNode, MsgTypes.empty, passStateNodes)

    transition.label shouldBe Some("repeat")
    transition.targetStateID shouldBe "pass-state-01"
    transition.isAuto shouldBe false
  }

  test("auto edge") {
    val nodeXML = createXMLEdge("graphml-edge-x", "graphml-state-01", "graphml-state-01", "automatisch", RGBColor.green)

    val edgeNode = Helper.parseEdge(nodeXML, keys, stateNodes)

    val s = "FIXME"
    val (_, transition) = GraphMLParser.parseTransition(s, edgeNode, MsgTypes.empty, passStateNodes)

    transition.label shouldBe Some("automatisch")
    transition.targetStateID shouldBe "pass-state-01"
    transition.isAuto shouldBe true
  }

  test("timeout edge") {
    val nodeXML = createXMLEdge("graphml-edge-x", "graphml-state-01", "graphml-state-01", "21s", RGBColor.blue)

    val edgeNode = Helper.parseEdge(nodeXML, keys, stateNodes)

    val s = "FIXME"
    val (_, transition) = GraphMLParser.parseTransition(s, edgeNode, MsgTypes.empty, passStateNodes)

    transition.getTimeout shouldBe 21
    //transition.isAuto shouldBe false // is implicit auto
  }

  test("cancel edge") {
    val nodeXML = createXMLEdge("graphml-edge-x", "graphml-state-01", "graphml-state-01", "abbruch", RGBColor.red)

    val edgeNode = Helper.parseEdge(nodeXML, keys, stateNodes)

    val s = "FIXME"
    val (_, transition) = GraphMLParser.parseTransition(s, edgeNode, MsgTypes.empty, passStateNodes)

    transition.cancel shouldBe true
    transition.hidden shouldBe false
  }

  test("hidden cancel edge") {
    val nodeXML = createXMLEdge("graphml-edge-x", "graphml-state-01", "graphml-state-01", "abbruch", RGBColor.red, "dashed")

    val edgeNode = Helper.parseEdge(nodeXML, keys, stateNodes)

    val s = "FIXME"
    val (_, transition) = GraphMLParser.parseTransition(s, edgeNode, MsgTypes.empty, passStateNodes)

    transition.cancel shouldBe true
    transition.hidden shouldBe true
  }

  ignore("autotimeout named edge") {
    val nodeXML = createXMLEdge("graphml-edge-x", "graphml-state-01", "graphml-state-01", "do stuff autotimeout:21s")

    val edgeNode = Helper.parseEdge(nodeXML, keys, stateNodes)

    val s = "FIXME"
    val (_, transition) = GraphMLParser.parseTransition(s, edgeNode, MsgTypes.empty, passStateNodes)

    transition.getTimeout shouldBe 21
    transition.isAuto shouldBe true
  }

  test("send edge simple") {
    val labelText = "ServiceOrder to Employee"
    val nodeXML = createXMLEdge("graphml-edge-x", "graphml-state-02", "graphml-state-01", labelText)

    val edgeNode = Helper.parseEdge(nodeXML, keys, stateNodes)

    val s = "FIXME"
    val msgTypes: MsgTypes = Map[(String, String), Set[String]]((s, "Employee") -> Set("ServiceOrder"))
    val (_, transition) = GraphMLParser.parseTransition(s, edgeNode, msgTypes, passStateNodes)

    transition.label shouldBe Some(labelText)
    transition.targetStateID shouldBe "pass-state-01"
    transition.communicationProperties.value.subject shouldBe "Employee"
    transition.communicationProperties.value.msgType shouldBe "ServiceOrder"
    transition.communicationProperties.value.subjectVar shouldBe ""
  }

  test("receive edge simple") {
    val labelText: String = "ServiceOrder from Employee"
    val nodeXML = createXMLEdge("graphml-edge-x", "graphml-state-03", "graphml-state-01", labelText)

    val edgeNode = Helper.parseEdge(nodeXML, keys, stateNodes)

    val s = "FIXME"
    val msgTypes: MsgTypes = Map[(String, String), Set[String]](("Employee", s) -> Set("ServiceOrder"))
    val (_, transition) = GraphMLParser.parseTransition(s, edgeNode, msgTypes, passStateNodes)

    transition.label shouldBe Some(labelText)
    transition.targetStateID shouldBe "pass-state-01"
    transition.communicationProperties.value.subject shouldBe "Employee"
    transition.communicationProperties.value.msgType shouldBe "ServiceOrder"
  }

  test("send edge to var") {
    val labelText: String = "Order to Contractor in voffer"
    val nodeXML = createXMLEdge("graphml-edge-x", "graphml-state-02", "graphml-state-01", labelText)

    val edgeNode = Helper.parseEdge(nodeXML, keys, stateNodes)

    val s = "FIXME"
    val msgTypes: MsgTypes = Map[(String, String), Set[String]]((s, "Contractor") -> Set("Order"))
    val (_, transition) = GraphMLParser.parseTransition(s, edgeNode, msgTypes, passStateNodes)

    transition.label shouldBe Some(labelText)
    transition.targetStateID shouldBe "pass-state-01"
    transition.communicationProperties.value.subject shouldBe "Contractor"
    transition.communicationProperties.value.msgType shouldBe "Order"
    transition.communicationProperties.value.subjectVar shouldBe "voffer"
  }

  test("send edge to count") {
    val labelText: String = "Order to 3 of Contractor"
    val nodeXML = createXMLEdge("graphml-edge-x", "graphml-state-02", "graphml-state-01", labelText)

    val edgeNode = Helper.parseEdge(nodeXML, keys, stateNodes)

    val s = "FIXME"
    val msgTypes: MsgTypes = Map[(String, String), Set[String]]((s, "Contractor") -> Set("Order"))
    val (_, transition) = GraphMLParser.parseTransition(s, edgeNode, msgTypes, passStateNodes)

    transition.label shouldBe Some(labelText)
    transition.targetStateID shouldBe "pass-state-01"
    transition.communicationProperties.value.subject shouldBe "Contractor"
    transition.communicationProperties.value.msgType shouldBe "Order"
    transition.communicationProperties.value.subjectCountMin shouldBe 3
    transition.communicationProperties.value.subjectCountMax shouldBe 3
  }

  test("send edge with correlation") {
    val labelText: String = "Order to Contractor with new correlation \"corrID\""
    val nodeXML = createXMLEdge("graphml-edge-x", "graphml-state-02", "graphml-state-01", labelText)

    val edgeNode = Helper.parseEdge(nodeXML, keys, stateNodes)

    val s = "FIXME"
    val msgTypes: MsgTypes = Map[(String, String), Set[String]]((s, "Contractor") -> Set("Order"))
    val (_, transition) = GraphMLParser.parseTransition(s, edgeNode, msgTypes, passStateNodes)

    transition.label shouldBe Some(labelText)
    transition.targetStateID shouldBe "pass-state-01"
    transition.communicationProperties.value.subject shouldBe "Contractor"
    transition.communicationProperties.value.msgType shouldBe "Order"
    transition.communicationProperties.value.new_correlation_var shouldBe "corrID"
  }

  test("receive edge storemessage") {
    val labelText: String = "Offer from 7 of Contractor =: voffer"
    val nodeXML = createXMLEdge("graphml-edge-x", "graphml-state-03", "graphml-state-01", labelText)

    val edgeNode = Helper.parseEdge(nodeXML, keys, stateNodes)

    val s = "FIXME"
    val msgTypes: MsgTypes = Map[(String, String), Set[String]](("Contractor", s) -> Set("Offer"))
    val (_, transition) = GraphMLParser.parseTransition(s, edgeNode, msgTypes, passStateNodes)

    transition.label shouldBe Some(labelText)
    transition.targetStateID shouldBe "pass-state-01"
    transition.communicationProperties.value.subject shouldBe "Contractor"
    transition.communicationProperties.value.msgType shouldBe "Offer"
    transition.communicationProperties.value.store_messages_var shouldBe "voffer"
    transition.communicationProperties.value.subjectCountMin shouldBe 7
    transition.communicationProperties.value.subjectCountMax shouldBe 7
  }

  test("receive edge storemessage quoted") {
    val labelText: String = "\"Test Offer\" from 7 of \"Test Contractor\" =: \"Test voffer\""
    val nodeXML = createXMLEdge("graphml-edge-x", "graphml-state-03", "graphml-state-01", labelText)

    val edgeNode = Helper.parseEdge(nodeXML, keys, stateNodes)

    val s = "FIXME"
    val msgTypes: MsgTypes = Map[(String, String), Set[String]](("Test Contractor", s) -> Set("Test Offer"))
    val (_, transition) = GraphMLParser.parseTransition(s, edgeNode, msgTypes, passStateNodes)

    transition.label shouldBe Some(labelText)
    transition.targetStateID shouldBe "pass-state-01"
    transition.communicationProperties.value.subject shouldBe "Test Contractor"
    transition.communicationProperties.value.msgType shouldBe "Test Offer"
    transition.communicationProperties.value.store_messages_var shouldBe "Test voffer"
    transition.communicationProperties.value.subjectCountMin shouldBe 7
    transition.communicationProperties.value.subjectCountMax shouldBe 7
  }

  test("receive edge storemessage quoted with correlation") {
    val labelText: String = "\"Test Offer\" with correlation of \"corrID\" from 7 of \"Test Contractor\" =: \"Test voffer\""
    val nodeXML = createXMLEdge("graphml-edge-x", "graphml-state-03", "graphml-state-01", labelText)

    val edgeNode = Helper.parseEdge(nodeXML, keys, stateNodes)

    val s = "FIXME"
    val msgTypes: MsgTypes = Map[(String, String), Set[String]](("Test Contractor", s) -> Set("Test Offer"))
    val (_, transition) = GraphMLParser.parseTransition(s, edgeNode, msgTypes, passStateNodes)

    transition.label shouldBe Some(labelText)
    transition.targetStateID shouldBe "pass-state-01"
    transition.communicationProperties.value.subject shouldBe "Test Contractor"
    transition.communicationProperties.value.msgType shouldBe "Test Offer"
    transition.communicationProperties.value.with_correlation_var shouldBe "corrID"
    transition.communicationProperties.value.store_messages_var shouldBe "Test voffer"
    transition.communicationProperties.value.subjectCountMin shouldBe 7
    transition.communicationProperties.value.subjectCountMax shouldBe 7
  }
}
