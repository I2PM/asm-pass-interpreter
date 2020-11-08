package de.athalis.pass.parser.graphml

import de.athalis.pass.parser.ast.pass.SubjectNode
import de.athalis.pass.parser.graphml.Helper.ParserLocation
import de.athalis.pass.parser.graphml.parser.GraphMLParser
import de.athalis.pass.parser.graphml.parser.GraphMLParser.MsgTypes
import de.athalis.pass.parser.graphml.structure.Key

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable

class SubjectNodeSpec extends AnyFunSuite with Matchers {
  private implicit val loc: ParserLocation = ParserLocation("SubjectNodeSpec", None)

  private val keys: immutable.Seq[Key[_]] = Util.keys

  test("minimal subject") {
    val nodeXML =
      <node id="n0">
        <data key="d6"><![CDATA[Subject]]></data>
        <data key="d7">
          <y:ProxyAutoBoundsNode>
            <y:Realizers active="1">
              <y:GroupNode>
                <y:NodeLabel modelName="internal">"Service Desk"</y:NodeLabel>
              </y:GroupNode>
              <y:GroupNode>
                <y:NodeLabel modelName="internal">"Service Desk"</y:NodeLabel>
              </y:GroupNode>
            </y:Realizers>
          </y:ProxyAutoBoundsNode>
        </data>
        <graph>
          <node id="n0::n0">
            <data key="d6"><![CDATA[Internal Action]]></data>
            <data key="d7">
              <y:GenericNode configuration="com.yworks.bpmn.Activity.withShadow">
                <y:Geometry height="55.0" width="227.0" x="-2935.387448437497" y="-656.1830624999997"/>
                <y:Fill color="#FFFFFFE6" color2="#D4D4D4CC" transparent="false"/>
                <y:BorderStyle color="#123EA2" type="line" width="3.0"/>
                <y:NodeLabel alignment="center" autoSizePolicy="content" fontFamily="Dialog" fontSize="12" fontStyle="plain" hasBackgroundColor="false" hasLineColor="false" height="17.96875" modelName="custom" textColor="#000000" visible="true" width="158.46484375" x="34.267578125" y="18.515625">Discover need for Service<y:LabelModel>
                  <y:SmartNodeLabelModel distance="4.0"/>
                </y:LabelModel>
                <y:ModelParameter>
                  <y:SmartNodeLabelModelParameter labelRatioX="0.0" labelRatioY="0.0" nodeRatioX="0.0" nodeRatioY="0.0" offsetX="0.0" offsetY="0.0" upX="0.0" upY="-1.0"/>
                </y:ModelParameter>
                </y:NodeLabel>
                <y:StyleProperties>
                <y:Property class="java.awt.Color" name="com.yworks.bpmn.icon.line.color" value="#000000"/>
                <y:Property class="com.yworks.yfiles.bpmn.view.TaskTypeEnum" name="com.yworks.bpmn.taskType" value="TASK_TYPE_SERVICE"/>
                <y:Property class="java.awt.Color" name="com.yworks.bpmn.icon.fill2" value="#d4d4d4cc"/>
                <y:Property class="java.awt.Color" name="com.yworks.bpmn.icon.fill" value="#ffffffe6"/>
                <y:Property class="com.yworks.yfiles.bpmn.view.BPMNTypeEnum" name="com.yworks.bpmn.type" value="ACTIVITY_TYPE"/>
                <y:Property class="com.yworks.yfiles.bpmn.view.ActivityTypeEnum" name="com.yworks.bpmn.activityType" value="ACTIVITY_TYPE_TASK"/>
                </y:StyleProperties>
              </y:GenericNode>
            </data>
          </node>
        </graph>
      </node>
    val node = Helper.parseNode(nodeXML, keys)

    val subject: SubjectNode = GraphMLParser.parseInternalSubject(node, MsgTypes.empty, None)(loc)

    subject.id shouldBe "Service Desk"
  }
}
