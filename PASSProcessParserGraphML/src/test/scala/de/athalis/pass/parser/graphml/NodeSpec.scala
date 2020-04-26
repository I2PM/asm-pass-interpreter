package de.athalis.pass.parser.graphml

import org.scalatest.FunSuite
import org.scalatest.Matchers
import org.scalatest.OptionValues._

class NodeSpec extends FunSuite with Matchers {

  val keys = Util.keys

  test("basic node") {
    val nodeXML = <node id="test" />
    val node = Helper.parseNode(nodeXML, keys)

    node.id shouldBe "test"
  }

  test("node description") {
    val nodeXML =
      <node id="test">
        <data key="d6"><![CDATA[Subject]]></data>
      </node>

    val node = Helper.parseNode(nodeXML, keys)

    node.id shouldBe "test"
    node.description.value shouldBe "Subject"
  }

  test("node label state") {
    val nodeXML =
      <node id="test">
        <data key="d7">
          <y:GenericNode configuration="com.yworks.bpmn.Activity.withShadow">
            <y:NodeLabel modelName="custom">Send</y:NodeLabel>
          </y:GenericNode>
        </data>
      </node>

    val node = Helper.parseNode(nodeXML, keys)

    node.id shouldBe "test"
    node.getLabel shouldBe Some("Send")
  }

  test("node label single subject") {
    val nodeXML =
      <node id="test">
        <data key="d7">
          <y:ProxyAutoBoundsNode>
            <y:Realizers active="1">
              <y:GroupNode>
                <y:NodeLabel modelName="internal">Principal</y:NodeLabel>
              </y:GroupNode>
              <y:GroupNode>
                <y:NodeLabel modelName="internal">Principal</y:NodeLabel>
              </y:GroupNode>
            </y:Realizers>
          </y:ProxyAutoBoundsNode>
        </data>
      </node>

    val node = Helper.parseNode(nodeXML, keys)

    node.id shouldBe "test"
    node.getLabel shouldBe Some("Principal")
  }

  test("node label multi subject") {
    val nodeXML =
      <node id="test">
        <data key="d7">
          <y:ProxyAutoBoundsNode>
            <y:Realizers active="1">
            <y:GroupNode>
              <y:NodeLabel modelName="internal">Contractor</y:NodeLabel>
            </y:GroupNode>
            <y:GroupNode>
              <y:NodeLabel modelName="internal">Contractor</y:NodeLabel>
              <y:NodeLabel modelName="custom">M</y:NodeLabel>
            </y:GroupNode>
            </y:Realizers>
          </y:ProxyAutoBoundsNode>
        </data>
      </node>

    val node = Helper.parseNode(nodeXML, keys)

    node.id shouldBe "test"
    node.getLabel shouldBe Some("Contractor")
    //Helper.findLabelText(node, "custom").value shouldBe "M"
  }
}
