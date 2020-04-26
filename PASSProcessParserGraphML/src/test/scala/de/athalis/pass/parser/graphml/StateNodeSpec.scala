package de.athalis.pass.parser.graphml

import scala.xml.{ Node => XMLNode }

import scala.collection.immutable._

import org.scalatest.FunSuite
import org.scalatest.Matchers
import org.scalatest.OptionValues._

import de.athalis.pass.parser.graphml.parser.GraphMLParser
import de.athalis.pass.parser.ast.pass.StateNode.StateType._

object StateNodeSpec {

  val keys = Util.keys

  def createXMLStateNode(id: String, labelText: String, taskType: String, description: Option[String] = None, borderWidth: Double = 1.0): XMLNode =
        <node id={id}>
          <data key="d5"/>
          {if (description.isDefined) {<data key="d6">{description.get}</data>}}
          <data key="d7">
            <y:GenericNode configuration="com.yworks.bpmn.Activity.withShadow">
              <y:Geometry height="55.0" width="227.0" x="-2935.387448437497" y="-656.1830624999997"/>
              <y:Fill color="#FFFFFFE6" color2="#D4D4D4CC" transparent="false"/>
              <y:BorderStyle color="#123EA2" type="line" width={borderWidth.toString} />
              <y:NodeLabel alignment="center" autoSizePolicy="content" fontFamily="Dialog" fontSize="12" fontStyle="plain" hasBackgroundColor="false" hasLineColor="false" height="17.96875" modelName="custom" textColor="#000000" visible="true" width="158.46484375" x="34.267578125" y="18.515625">{labelText}<y:LabelModel>
                  <y:SmartNodeLabelModel distance="4.0"/>
                </y:LabelModel>
                <y:ModelParameter>
                  <y:SmartNodeLabelModelParameter labelRatioX="0.0" labelRatioY="0.0" nodeRatioX="0.0" nodeRatioY="0.0" offsetX="0.0" offsetY="0.0" upX="0.0" upY="-1.0"/>
                </y:ModelParameter>
              </y:NodeLabel>
              <y:StyleProperties>
                <y:Property class="java.awt.Color" name="com.yworks.bpmn.icon.line.color" value="#000000"/>
                <y:Property class="com.yworks.yfiles.bpmn.view.TaskTypeEnum" name="com.yworks.bpmn.taskType" value={taskType} />
                <y:Property class="java.awt.Color" name="com.yworks.bpmn.icon.fill2" value="#d4d4d4cc"/>
                <y:Property class="java.awt.Color" name="com.yworks.bpmn.icon.fill" value="#ffffffe6"/>
                <y:Property class="com.yworks.yfiles.bpmn.view.BPMNTypeEnum" name="com.yworks.bpmn.type" value="ACTIVITY_TYPE"/>
                <y:Property class="com.yworks.yfiles.bpmn.view.ActivityTypeEnum" name="com.yworks.bpmn.activityType" value="ACTIVITY_TYPE_TASK"/>
              </y:StyleProperties>
            </y:GenericNode>
          </data>
        </node>

  val minimalActionXML = createXMLStateNode("graphml-state-01", "Discover need for Service", "TASK_TYPE_SERVICE", Some("Internal Action"), 3.0)
  val minimalActionNode = Helper.parseNode(minimalActionXML, keys)
  val minimalActionState = GraphMLParser.parseState(minimalActionNode, "pass-state-01")

  val sendXML = createXMLStateNode("graphml-state-02", "Send", "TASK_TYPE_SEND")
  val sendNode = Helper.parseNode(sendXML, keys)
  val sendState = GraphMLParser.parseState(sendNode, "pass-state-02")

  val receiveXML = createXMLStateNode("graphml-state-03", "Receive", "TASK_TYPE_RECEIVE")
  val receiveNode = Helper.parseNode(receiveXML, keys)
  val receiveState = GraphMLParser.parseState(receiveNode, "pass-state-03")



  val nodes = Seq(minimalActionNode, sendNode, receiveNode)
  val nodeStates = Map(
      "graphml-state-01" -> minimalActionState,
      "graphml-state-02" -> sendState,
      "graphml-state-03" -> receiveState
    )
}

class StateNodeSpec extends FunSuite with Matchers {
  import StateNodeSpec._

  test("minimal action state") {
    minimalActionNode.id shouldBe "graphml-state-01"
    minimalActionState.id shouldBe "pass-state-01"
    minimalActionState.stateType shouldBe InternalAction
    minimalActionState.label.value shouldBe "Discover need for Service"
  }

  test("send state") {
    sendNode.id shouldBe "graphml-state-02"
    sendState.id shouldBe "pass-state-02"
    sendState.stateType shouldBe Send
    sendState.label.value shouldBe "Send"
  }

  test("receive state") {
    receiveNode.id shouldBe "graphml-state-03"
    receiveState.id shouldBe "pass-state-03"
    receiveState.stateType shouldBe Receive
    receiveState.label.value shouldBe "Receive"
  }
}
